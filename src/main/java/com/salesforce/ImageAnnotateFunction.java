package com.salesforce;

import com.salesforce.functions.jvm.sdk.Context;
import com.salesforce.functions.jvm.sdk.InvocationEvent;
import com.salesforce.functions.jvm.sdk.SalesforceFunction;
import com.salesforce.functions.jvm.sdk.data.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ImageAnnotateFunction implements SalesforceFunction<String, String> {
    @Override
    public String apply(InvocationEvent<String> event, Context context) throws Exception {
        DataApi dataApi = context.getOrg().get().getDataApi();
        File temporaryFile = File.createTempFile("function", ".png");

        RecordQueryResult queryResult = dataApi.query("SELECT VersionData FROM ContentVersion WHERE Title = '" + event.getData() + "'");

        for (Record record : queryResult.getRecords()) {
            ByteBuffer imageData = record.getBinaryField("VersionData").orElseThrow(() -> new IllegalStateException("Expected VersionData field!"));

            BufferedImage image = ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(imageData.array())));

            Graphics g = image.getGraphics();
            g.setColor(Color.CYAN);
            g.setFont(new Font("TimesRoman", Font.BOLD, 120));
            g.drawString("Hello World!", 10, 300);

            ImageIO.write(image, "png", temporaryFile);
        }

        RecordModificationResult createResult = dataApi.create(dataApi.newRecordBuilder("ContentVersion")
                .withField("Title", "SDK Uploaded Data")
                .withField("PathOnClient", "file.png")
                .withField("ContentLocation", "S")
                .withField("VersionData", ByteBuffer.wrap(Files.readAllBytes(temporaryFile.toPath())))
                .build());

        return createResult.getId();
    }
}
