package one.jodi.core.automapping.impl;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    public static void main(String[] args) {
        Pattern pattern = Pattern.compile(args[0], Pattern.CASE_INSENSITIVE);
        for (int i = 1; i < args.length; i++) {
            for (int j = i; j < args.length; j++) {
                Matcher matcher1 = pattern.matcher(args[i]);
                Matcher matcher2 = pattern.matcher(args[j]);

                if (matcher1.find() && matcher2.find() && matcher1.group().equalsIgnoreCase(matcher2.group())) {
                    System.out.println(args[i] + " \t\t\t" + args[j]);
                } else {
                    //System.out.println("failed\t" + args[i] + " " + args[j]);
                }
            }
        }
    }

} 