package com.test.ib.fixmessage.components;

import com.test.ib.fixmessage.utils.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class FileGenerator implements CommandLineRunner {

    private static final int NUMBER_OF_EXECUTIONS = 5000;
    private static final int NUMBER_OF_PARTIAL_EXECUTIONS = 2500;
    private static final String[] ACCOUNTS = {"111111", "222222", "333333", "444444", "555555",
            "666666", "777777", "888888", "999999", "101010"};
    private static final String[] SYMBOLS = {"BIDI11", "BPAN4", "BBSE3", "BRML3", "BBDC3", "BBDC4",
            "BRAP4", "BBAS3", "BRKM5", "PETR4"};

    private static final Random RND = new Random();

    @Autowired
    private FileManager fileManager;

    private static List<Message> generateMessages() throws FieldNotFound {

        int countPartial = 0;
        List<Message> messages = new ArrayList<>();

        for (int i = 1; i <= NUMBER_OF_EXECUTIONS; i++) {
            boolean isPartial = ++countPartial <= NUMBER_OF_PARTIAL_EXECUTIONS;
            Message message = generateMessage(i, isPartial, null);
            generateHeader(message, i);
            messages.add(message);
            if (isPartial) {
                Message secondMessage = generateMessage(++i, true, message);
                countPartial++;
                generateHeader(secondMessage, i);
                messages.add(secondMessage);
            }
        }
        return messages;
    }

    private static Message generateMessage(int messageNumber, boolean isPartial, Message parent) throws FieldNotFound {
        Message message = new Message();
        message.setField(new ClOrdID("009FDS8HGA"));
        message.setField(new SecurityIDSource(SecurityIDSource.EXCHANGE_SYMBOL));
        message.setField(new OrdType(OrdType.LIMIT));
        message.setField(new SecurityID("20000 0581991"));
        message.setField(new ExecType(ExecType.TRADE));
        message.setField(new SecurityExchange("BVMF"));
        message.setField(new TradingSessionID(TradingSessionID.DAY));
        message.setField(new AccountType(39));  // enum ???
        message.setField(new TradingSessionSubID(TradingSessionSubID.TRADING));
        message.setField(new AggressorIndicator(AggressorIndicator.ORDER_INITIATOR_IS_PASSIVE));
        message.setField(new ApplID("GATEWAY"));
        message.setField(new NoContraBrokers(1));
        message.setField(new ContraBroker("8"));
        message.setField(new PartyRole(PartyRole.ENTERING_TRADER));
        message.setField(new PartyID("RRH"));
        message.setField(new ExecID(UUID.randomUUID().toString()));
        message.setField(new OrderID(String.valueOf(9280000 + messageNumber)));
        message.setField(new OrdStatus(isPartial ? OrdStatus.PARTIALLY_FILLED : OrdStatus.FILLED));
        message.setField(new TransactTime(LocalDateTime.now()));
        message.setField(new TradeDate(String.valueOf(LocalDate.now())));

        if (parent == null) {
            generateQuantities(message, isPartial, null);
            generatePriceFields(message, null);
            // key to our last case
            message.setField(new Account(generateAccount()));
            message.setField(new Side(isPartial ? Side.BUY : Side.SELL));
            message.setField(new Symbol(generateSymbol()));
        } else {
            generateQuantities(message, isPartial, parent);
            generatePriceFields(message, parent);
            // key to our last case
            message.setField(new Account(parent.getString(1)));
            message.setField(new Side(parent.getChar(54)));
            message.setField(new Symbol(parent.getString(55)));
        }
//        System.out.println(message);
        return message;
    }

    private static void generateHeader(Message message, int messageNumber) {
        Message.Header header = message.getHeader();
        header.setField(new BeginString("FIX.4.4"));
        header.setField(new MsgType(MsgType.EXECUTION_REPORT));
        header.setField(new SenderCompID("SISTEMA"));
        header.setField(new TargetCompID("DROPCOPY"));
        header.setField(new DeliverToCompID("GATEWAY"));
        header.setField(new MsgSeqNum(messageNumber));
        header.setField(new SendingTime(LocalDateTime.now()));
    }

    private static void generateQuantities(Message message, boolean isPartial, Message initialOrder) throws FieldNotFound {
        int orderQty = initialOrder == null ? (int) ((Math.random() * (10000 - 1000)) + 1000) : initialOrder.getInt(38);
        int cumQty = initialOrder == null ? 0 : initialOrder.getInt(14);
        int lastQty = isPartial ? RND.nextInt(orderQty - cumQty - 100) : orderQty;
        cumQty += lastQty;
        message.setField(new OrderQty(orderQty));
        message.setField(new LastQty(lastQty));
        message.setField(new CumQty(cumQty));
        if (isPartial) {
            int leavesQty = orderQty - cumQty;
            message.setField(new LeavesQty(leavesQty));
        } else {
            message.setField(new LeavesQty(0));
        }
    }

    private static void generatePriceFields(Message message, Message initialOrder) throws FieldNotFound {
        double price = initialOrder == null ? RND.nextInt(1000) / 10.0 : initialOrder.getDouble(44) * 0.985;
        message.setField(new LastPx(price));
        message.setField(new Price(price));
        double avg = initialOrder == null ? price : (price + initialOrder.getDouble(31)) / 2;
        message.setField(new AvgPx(avg));
    }

    private static String generateAccount() {
        return ACCOUNTS[RND.nextInt(ACCOUNTS.length)];
    }

    private static String generateSymbol() {
        return SYMBOLS[RND.nextInt(SYMBOLS.length)];
    }

    @Override
    public void run(String... args) throws Exception {
        Instant start = Instant.now();
        List<Message> messages = generateMessages();
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("FileGenerator : data generation : timeElapsed : %d ms", timeElapsed));
        start = Instant.now();
        FileManager.writeBuffered(messages, "executions.txt");
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("FileGenerator : %s : timeElapsed : %d ms",
                System.getProperty("java.io.tmpdir")+"/executions.txt", timeElapsed));
    }

}
