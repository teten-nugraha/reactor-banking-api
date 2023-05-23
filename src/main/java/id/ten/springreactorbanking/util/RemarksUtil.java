package id.ten.springreactorbanking.util;

import java.math.BigDecimal;

public class RemarksUtil {

    public static String transferRemarks(String source, String destination, BigDecimal amount) {
        return "Transfer "+source+" -> "+destination+ " ["+amount+"]";
    }

    public static String depositRemarks(String ower, BigDecimal amount) {
        return "Deposit " +ower+ " ["+amount+"]";
    }

    public static String withdrawRemarks(String ower, BigDecimal amount) {
        return "Withdraw " +ower+ " [-"+amount+"]";
    }

}
