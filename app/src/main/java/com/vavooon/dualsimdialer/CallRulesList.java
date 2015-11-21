package com.vavooon.dualsimdialer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by Vavooon on 13.10.2015.
 */
public class CallRulesList extends Application {
    private ArrayList<CallRule> rulesList = new ArrayList<CallRule>();
    private ArrayList<Pattern> patternsList = new ArrayList<Pattern>();
    private static CallRulesList singleton = null;
    private Context context;
    SharedPreferences prefs;
    private static final String TAG = "xposed_debug";

    private void generatePatterns() {
        patternsList.clear();
        for (int i = 0; i<rulesList.size(); i++) {
            String patternString = rulesList.get(i).getRuleString();
            patternString = patternString.replace("#", ".").replace(",", "|").replace("+", "\\+");
            Pattern p = Pattern.compile(patternString);
            patternsList.add(p);
        }
    }

    public CallRulesList (Context c, SharedPreferences p) {
        context = c;
        singleton = this;
        Log.e("xposed_debug", "init");
        if (p != null) {
            prefs = p;
        }
        loadData();
    }



    public static CallRulesList getInstance() {
        if(singleton == null) {
            throw new Error("Please initialize singleton with proper context");
        }
        return singleton;
    }

    void addRule(CallRule rule) {
        rulesList.add(rule);
        generatePatterns();
        saveData();
    }

    void updateRule(int id, CallRule rule) {
        rulesList.set(id, rule);
        generatePatterns();
        saveData();
    }

    void removeRule (CallRule rule) {
        rulesList.remove(rule);
        generatePatterns();
        saveData();
    }

    void removeRule (int index) {
        rulesList.remove(index);
        generatePatterns();
        saveData();
    }

    int size () {
        return rulesList.size();
    }

    CallRule get (int index) {
        return rulesList.get(index);
    }

    public void loadData (String value) {
        if (value != null) {
            Log.e("loadData", value);
            String[] rules = value.split("\\|");
            rulesList.clear();
            for (int i = 0; i < rules.length; i++) {
                Log.e("loadDataRule" + i, rules[i]);
                String[] rule = rules[i].split("\\:");
                int cardId = Integer.parseInt(rule[0]);
                String ruleString = rule[1];
                rulesList.add(new CallRule(cardId, ruleString));
            }
            generatePatterns();
        }
    }

    private void loadData () {
        String value;
        if (prefs != null ) {
             value = prefs.getString("rulesList", null);
        }
        else {
            value = "0:(\\+38,38,8)0(63,73,93)#######";
            //value = "0:(\\+38,38,8)0(63,73,93)#######|1:(\\+38,38,8)0(67,68,96,97,98)#######";
        }
        loadData(value);
    }

    public void saveData () {
        SharedPreferences.Editor editor = prefs.edit();
        String encodedRules = "";
        CallRule rule;
        for (int i = 0; i<rulesList.size(); i++) {
            rule = rulesList.get(i);
            encodedRules += "" + rule.cardId + ":" + rule.ruleString + "|";
        }
        editor.putString("rulesList", encodedRules.replaceFirst(".$", ""));
        editor.commit();
    }

    public PhoneAccountHandle getPhoneAccountHandleForNumber(String uri) {
        TelecomManager telecomManager =
                (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccountHandle> availablePhoneAccountHandles = telecomManager.getCallCapablePhoneAccounts();

        Log.e(TAG, "Check all filters(" + patternsList.size() + ")");
        for (int i = 0; i<patternsList.size(); i++) {
            Matcher matcher = patternsList.get(i).matcher(uri);
            Log.e(TAG, patternsList.get(i).pattern());
            if(matcher.find())
            {
                return availablePhoneAccountHandles.get(rulesList.get(i).cardId);
            }
        }
        return null;
    }
}
