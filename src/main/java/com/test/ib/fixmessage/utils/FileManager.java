package com.test.ib.fixmessage.utils;

import com.test.ib.fixmessage.dto.Execution;
import org.springframework.stereotype.Component;
import quickfix.InvalidMessage;
import quickfix.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileManager {

    public static void writeBuffered(List<Message> records, String filename) throws IOException {
        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, filename);
        FileWriter writer = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(writer, 8192);
        write(records, bufferedWriter);
    }

    private static void write(List<Message> records, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Message record : records) {
            sb.append(record).append(System.lineSeparator());
        }
        sb.deleteCharAt(sb.length() - 1);
        writer.write(sb.toString());
        writer.close();
    }

    public static void writeBufferedAllExecutions(List<Execution> records, String fileName) throws IOException {
        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, fileName);
        FileWriter writer = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(writer, 8192);
        writeExecution(records, bufferedWriter);
        writer.close();
    }

    private static void writeExecution(List<Execution> records, Writer writer) throws IOException {
        for (Execution record : records) {
            writer.write(String.valueOf(record));
            writer.write(System.lineSeparator());
        }
        writer.close();
    }

    public static void writeBufferedCSV(List<String> records, String filename) throws IOException {
        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, filename);
        FileWriter writer = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(writer, 8192);
        writeCSV(records, bufferedWriter);
    }

    private static void writeCSV(List<String> records, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String record : records) {
            sb.append(record).append(System.lineSeparator());
        }
        sb.deleteCharAt(sb.length() - 1);
        writer.write(sb.toString());
        writer.close();
    }

    public List<Message> readFixExecutionsFromFile(File file) throws IOException {

        // read file with fix strings and create a list with quickfix messages
        List<String> executions;
        try (Stream<String> lines = Files.lines(Paths.get(file.toString()))) {
            executions = lines.collect(Collectors.toList());
        }
        return executions.stream().map(x -> {
                    Message msg = null;
                    try {
                        msg = new Message(x);
                    } catch (InvalidMessage e) {
                        e.printStackTrace();
                    }
                    return msg;
                })
                .collect(Collectors.toList());
    }

    public List<Execution> readExecutionsFromFile(File file) throws IOException {

        // read file with strings and create a list with Executions
        List<String> executions;
        try (Stream<String> lines = Files.lines(Paths.get(file.toString()))) {
            executions = lines.collect(Collectors.toList());
        }
        return executions.stream().map(x -> {
            String[] fields = x.split(",");
            Execution exec = new Execution();
            exec.setTimestamp(LocalDateTime.parse(fields[0]));
            exec.setAccount(fields[1]);
            exec.setInstrument(fields[2]);
            exec.setSide(fields[3].charAt(0));
            exec.setOrderQuantity(Long.parseLong(fields[4]));
            exec.setActualExecutedQuantity(Long.parseLong(fields[5]));
            exec.setAccumulatedExecutedQuantity(Long.parseLong(fields[6]));
            exec.setExecutionPrice(Double.parseDouble(fields[7]));
            exec.setOrderNotional(Double.parseDouble(fields[8]));
            exec.setActualExecutionNotional(Double.parseDouble(fields[9]));
            exec.setAccumulatedExecutionNotional(Double.parseDouble(fields[10]));
            exec.setEnteringTrader(fields[11]);
            return exec;
        }).collect(Collectors.toList());
    }

}
