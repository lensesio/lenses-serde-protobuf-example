package io.lenses.examples.serde;


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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Program {
    public static void main(String[] arg) throws IOException {
        ProtobufData protobufData = ProtobufData.get();
        Schema avroSchema = protobufData.getSchema(CardData.CreditCard.class);

        CardData.CreditCard cc = CardData.CreditCard.newBuilder()
                .setBlocked(false)
                .setCardNumber("1111")
                .setType("VISA")
                .setCurrency("GBP")
                .setCountry("UK")
                .setName("John Snow")
                .build();

        ProtobufDatumWriter<CardData.CreditCard> pbWriter = new ProtobufDatumWriter<CardData.CreditCard>(avroSchema);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        pbWriter.write(cc, encoder);
        encoder.flush();
        GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(avroSchema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(out.toByteArray(), null);
        GenericRecord record = datumReader.read(null, decoder);
        System.out.println(record);

    }
}
