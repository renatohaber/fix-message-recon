package com.test.ib.fixmessage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.*;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "ExecutionBuilder", toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Execution.ExecutionBuilder.class)
public class Execution {

    private final DecimalFormat df2 = new DecimalFormat("#.##");

    // added order id to help my debug of partial orders
    private String orderId;

    private LocalDateTime timestamp;
    private String account;
    private String instrument;
    private Character side;
    private long orderQuantity;
    private long actualExecutedQuantity;
    private long accumulatedExecutedQuantity;
    private double executionPrice;
    private double orderNotional;
    private double actualExecutionNotional;
    private double accumulatedExecutionNotional;
    private String enteringTrader;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        //sb.append(orderId).append(",");
        sb.append(timestamp).append(",");
        sb.append(account).append(",");
        sb.append(instrument).append(",");
        sb.append(side).append(",");
        sb.append(orderQuantity).append(",");
        sb.append(actualExecutedQuantity).append(",");
        sb.append(accumulatedExecutedQuantity).append(",");
        sb.append(df2.format(executionPrice)).append(",");
        sb.append(df2.format(orderNotional)).append(",");
        sb.append(df2.format(actualExecutionNotional)).append(",");
        sb.append(df2.format(accumulatedExecutionNotional)).append(",");
        sb.append(enteringTrader);
        return sb.toString();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecutionBuilder {
    }
}