package io.lenses.examples.serde;

import com.landoop.lenses.lsql.serde.Deserializer;
import com.landoop.lenses.lsql.serde.Serde;
import com.landoop.lenses.lsql.serde.Serializer;
import io.lenses.examples.serde.protobuf.generated.CardData;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.protobuf.ProtobufData;
import org.apache.avro.protobuf.ProtobufDatumWriter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class CreditCardAutoProtobufSerde implements Serde {
    private final static Schema schema = ProtobufData.get().getSchema(CardData.CreditCard.class);

    @Override
    public Serializer serializer(Properties properties) {
        throw new NotImplementedException();
    }

    @Override
    public Deserializer deserializer(Properties properties) {
        return new Deserializer() {
            @Override
            public GenericRecord deserialize(byte[] bytes) throws IOException {

                CardData.CreditCard card = CardData.CreditCard.parseFrom(bytes);

                ProtobufDatumWriter<CardData.CreditCard> pbWriter = new ProtobufDatumWriter<CardData.CreditCard>(schema);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
                pbWriter.write(card, encoder);
                encoder.flush();
                GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
                Decoder decoder = DecoderFactory.get().binaryDecoder(out.toByteArray(), null);
                GenericRecord record = datumReader.read(null, decoder);
                return record;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public Schema getSchema() {
        return schema;
    }
}
