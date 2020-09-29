# Details

Provides an example on how to make Apache Kafka data stored as Google Protobuf visible.


## Protobuf schema used

The project uses a Protobuf schema stored in credit_card.proto to generate the Java classes.

```
syntax = "proto2";

package io.lenses.examples.serde.protobuf;

option java_package = "io.lenses.examples.serde.protobuf.generated";
option java_outer_classname = "CardData";

message CreditCard {
  required string name = 1;
  required string country = 2;
  required string currency = 3;
  required string cardNumber = 4;
  required bool blocked = 5;
  required string type = 6;

}
```

Using the maven plugin `protoc-jar-maven-plugin` the Java classes are generated under 
src/io/lenses/examples/serde/protobuf/generated.

```xml
 <plugin>
    <groupId>com.github.os72</groupId>
    <artifactId>protoc-jar-maven-plugin</artifactId>
    <version>3.2.0.1</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <inputDirectories>
                    <include>src/main/proto</include>
                </inputDirectories>
                <outputTargets>
                    <outputTarget>
                        <type>java</type>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                </outputTargets>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Lenses Plugin

Lenses exposes an interface `Serde` which needs to be implemented and then the artifacts dropped into the installation folder. 
Check http://docs.lenses.io for details.

```xml
 <dependency>
    <groupId>com.landoop</groupId>
    <artifactId>lenses-serde</artifactId>
    <version>${lenses.serdes.version}</version>
</dependency>
``` 

```java
    @Override
    public Serializer serializer(Properties properties) {
        //not required
        throw new NotImplementedException();
    }

    @Override
    public Deserializer deserializer(Properties properties) {
        // REQUIRED
    }

    @Override
    public Schema getSchema() {
        // REQUIRED
    }
```

The plugin implementation has to code the two methods:
 * `getSchema()` -  describes the payload structure. Returns Avro Schema
 * `deserializer(Properties properties)` - contains the logic to translate the raw bytes stored in Kafka as Avro GenericRecord
 
At the moment, the `serializer(Properties properties)` is not required (not used).


## Implementation

For the example there are two implementations provided:
 * CreditCardAutoProtobufSerde
 * CreditCardProtobufSerde

The first class is more generic, however not as performant. 
The second one is the reverse - it yields better performance at the expense of more coding.

#### CreditCardAutoProtobufSerde

Returning the Avro schema relies on entirely Avro library to extract it from the generated Protobuf classes


```java

    private final static Schema schema = ProtobufData.get().getSchema(CardData.CreditCard.class);

    @Override
    public Schema getSchema() {
        return schema;
    }
```  

Deserialization code takes the Kafka payload, which is raw bytes, and lifts it into a `GenericRecord`.
First the raw bytes are translated via the Google API to the `CreditCard`:

```java
    CardData.CreditCard card = CardData.CreditCard.parseFrom(bytes);
```

Next, the card details are written to an in memory array as Avro:

```java
ProtobufDatumWriter<CardData.CreditCard> pbWriter = new ProtobufDatumWriter<CardData.CreditCard>(schema);
ByteArrayOutputStream out = new ByteArrayOutputStream();
Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
pbWriter.write(card, encoder);
encoder.flush();
```

Last step is to read the Avro bytes as `GenericRecord`:

```java
GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
Decoder decoder = DecoderFactory.get().binaryDecoder(out.toByteArray(), null);
GenericRecord record = datumReader.read(null, decoder);
return record;
```


#### CreditCardProtobufSerde

Going from bytes to `CreditCard` to bytes to `GenericRecord` can be short-circuited by avoiding the intermediary bytes set.
This is where this second implementation comes into play.

For this implementation the intermediary step is skipped. 
This means there needs to be more manual code to populate the `GenericRecord`.
 
```java
  @Override
    public Deserializer deserializer(Properties properties) {
        return new Deserializer() {
            @Override
            public GenericRecord deserialize(byte[] bytes) throws IOException {

                CardData.CreditCard card = CardData.CreditCard.parseFrom(bytes);

                GenericRecord record = new GenericData.Record(schema);
                record.put("name", card.getName());
                record.put("cardNumber", card.getCardNumber());
                record.put("cardType", card.getType());
                record.put("country", card.getCountry());
                record.put("currency", card.getCurrency());
                record.put("blocked", card.getBlocked());
                return record;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }
```

If the structure were to be nested say `NestedObj`:
```
{
  "a": 123,
  "b": {
    "x":"value1"
  }
}
```

Then the code has to create a `GenericRecord` for the `b` field and one for the `NestedObject`. 
First step is to create the schemas:

```
private Schema fieldBSchema = SchemaBuilder.builder()
        .record("field_b")
        .fields()
        .requiredString("x")
        .endRecord()
    
private Schema schema = SchemaBuilder.builder()
        .record("nested_obj")
        .fields()
        .requiredInt("a")
        .name("b")
            .type(fieldBSchema)
            .noDefault()
        .endRecord();
```

Next, the deserializer code needs to create and populate the `GenericRecord`[-s]:

```java
  @Override
    public Deserializer deserializer(Properties properties) {
        return new Deserializer() {
            @Override
            public GenericRecord deserialize(byte[] bytes) throws IOException {

                NestedObj obj = NestedObj.parseFrom(bytes);

                GenericRecord record = new GenericData.Record(schema);
                record.put("a", obj.getA());

                GenericRecord recordFieldB = new GenericData.Record(fieldBSchema);
                recordFieldB.put("x", obj.getB().getX());
                record.put("b", recordFieldB);
                
                return record;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }
```

## Build and deploy

To compile:
`mvn clean compile`

To create the jar:

`mvn clean package`

Follow the docs(https://docs.lenses.io/4.0/configuration/sql/kubernetes/#custom-serde)
 and provide to Lenses:
```
   target/lenses-serde-protobuf-example-1.0.0.jar
   deps/avro-protobuf-1.8.2.jar
```