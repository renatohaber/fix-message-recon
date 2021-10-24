package com.test.ib.fixmessage.components;

import com.test.ib.fixmessage.dto.Execution;
import com.test.ib.fixmessage.utils.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.Message;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
@Slf4j
public class AllMsgsGenerator implements CommandLineRunner {

    @Autowired
    private FileManager fileManager;

    private static Execution toExecution(Message message) throws FieldNotFound {
        final int qty = message.getInt(32);
        final int cumqty = message.getInt(14);
        final double px = message.getDouble(44);
        final double avgpx = message.getDouble(6);
        final double notional = qty * px;
        final double execNotional = qty * avgpx;
        final double cumNotional = cumqty * avgpx;
        return Execution.builder()
                .orderId(message.getString(37))
                .timestamp(message.getUtcTimeStamp(60))
                .account(message.getString(1))
                .instrument(message.getString(55))
                .side(message.getChar(54))
                .orderQuantity(message.getInt(38))
                .actualExecutedQuantity(qty)
                .accumulatedExecutedQuantity(cumqty)
                .executionPrice(px)
                .orderNotional(notional)
                .actualExecutionNotional(execNotional)
                .accumulatedExecutionNotional(cumNotional)
                .enteringTrader(message.getString(448))
                .build();
    }

    @Override
    public void run(String... args) throws Exception {

        Instant start = Instant.now();
        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, "executions.txt");

        List<Message> executionsFix = fileManager.readFixExecutionsFromFile(file);

        // generate CSV reduced list of executions
        // prepare list of fulfilled orders with requested rules
        List<Execution> result = new ArrayList<>();
        List<Message> fullFillExecutions = new ArrayList<>();
        for (Message message : executionsFix) {
            try {
                Execution exec = toExecution(message);
                result.add(exec);
                int leavesQty = message.getInt(151);
                // don't wanna to read the input file twice
                if (leavesQty == 0) {
                    message.setDouble(1010, exec.getOrderNotional());
                    message.setString(1011, exec.getEnteringTrader());
                    fullFillExecutions.add(message);
                }
            } catch (FieldNotFound e) {
                e.printStackTrace();
            }
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("AllMsgsGenerator | FulfillGenerator : data generation : timeElapsed : %d ms", timeElapsed));

        // AllMsgs.csv
        start = Instant.now();
        FileManager.writeBufferedAllExecutions(result, "AllMsgs.csv");
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("AllMsgs : %s : timeElapsed : %d ms",
                System.getProperty("java.io.tmpdir")+"/AllMsgs.csv", timeElapsed));

        // write FulFill
        start = Instant.now();
        FileManager.writeBuffered(fullFillExecutions, "FulFill.txt");
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("FulFill : %s : timeElapsed : %d ms",
                System.getProperty("java.io.tmpdir")+"/FulFill.txt", timeElapsed));

    }
}
