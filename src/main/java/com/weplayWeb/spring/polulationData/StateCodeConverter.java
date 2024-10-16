package com.weplayWeb.spring.polulationData;

import java.util.HashMap;
import java.util.Map;

public class StateCodeConverter {

    private static final Map<String, String> stateCodeMap = new HashMap<>();

    static {
        stateCodeMap.put("Alabama", "01");
        stateCodeMap.put("Alaska", "02");
        stateCodeMap.put("Arizona", "04");
        stateCodeMap.put("Arkansas", "05");
        stateCodeMap.put("California", "06");
        stateCodeMap.put("Colorado", "08");
        stateCodeMap.put("Connecticut", "09");
        stateCodeMap.put("Delaware", "10");
        stateCodeMap.put("Florida", "12");
        stateCodeMap.put("Georgia", "13");
        stateCodeMap.put("Hawaii", "15");
        stateCodeMap.put("Idaho", "16");
        stateCodeMap.put("Illinois", "17");
        stateCodeMap.put("Indiana", "18");
        stateCodeMap.put("Iowa", "19");
        stateCodeMap.put("Kansas", "20");
        stateCodeMap.put("Kentucky", "21");
        stateCodeMap.put("Louisiana", "22");
        stateCodeMap.put("Maine", "23");
        stateCodeMap.put("Maryland", "24");
        stateCodeMap.put("Massachusetts", "25");
        stateCodeMap.put("Michigan", "26");
        stateCodeMap.put("Minnesota", "27");
        stateCodeMap.put("Mississippi", "28");
        stateCodeMap.put("Missouri", "29");
        stateCodeMap.put("Montana", "30");
        stateCodeMap.put("Nebraska", "31");
        stateCodeMap.put("Nevada", "32");
        stateCodeMap.put("New Hampshire", "33");
        stateCodeMap.put("New Jersey", "34");
        stateCodeMap.put("New Mexico", "35");
        stateCodeMap.put("New York", "36");
        stateCodeMap.put("North Carolina", "37");
        stateCodeMap.put("North Dakota", "38");
        stateCodeMap.put("Ohio", "39");
        stateCodeMap.put("Oklahoma", "40");
        stateCodeMap.put("Oregon", "41");
        stateCodeMap.put("Pennsylvania", "42");
        stateCodeMap.put("Rhode Island", "44");
        stateCodeMap.put("South Carolina", "45");
        stateCodeMap.put("South Dakota", "46");
        stateCodeMap.put("Tennessee", "47");
        stateCodeMap.put("Texas", "48");
        stateCodeMap.put("Utah", "49");
        stateCodeMap.put("Vermont", "50");
        stateCodeMap.put("Virginia", "51");
        stateCodeMap.put("Washington", "53");
        stateCodeMap.put("West Virginia", "54");
        stateCodeMap.put("Wisconsin", "55");
        stateCodeMap.put("Wyoming", "56");
        // Add other states as needed
    }

    public static String getStateCode(String stateName) {
        return stateCodeMap.getOrDefault(stateName, "State not found");
    }

    public static void main(String[] args) {
        String stateName = "Michigan";
        String stateCode = getStateCode(stateName);
        System.out.println("The state code for " + stateName + " is: " + stateCode);
    }
}
