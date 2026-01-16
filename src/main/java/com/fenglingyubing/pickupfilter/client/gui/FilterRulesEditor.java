package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.config.FilterRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FilterRulesEditor {
    private final List<FilterRule> rules = new ArrayList<>();
    private int selectedIndex = -1;

    public synchronized void replaceAll(List<FilterRule> newRules) {
        rules.clear();
        Set<FilterRule> unique = new LinkedHashSet<>();
        if (newRules != null) {
            for (FilterRule rule : newRules) {
                if (rule != null) {
                    unique.add(rule);
                }
            }
        }
        rules.addAll(unique);
        selectedIndex = rules.isEmpty() ? -1 : Math.min(selectedIndex, rules.size() - 1);
    }

    public synchronized List<FilterRule> getRules() {
        return Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public synchronized int getSelectedIndex() {
        return selectedIndex;
    }

    public synchronized void setSelectedIndex(int index) {
        if (index < 0 || index >= rules.size()) {
            selectedIndex = -1;
            return;
        }
        selectedIndex = index;
    }

    public synchronized boolean addRule(FilterRule rule) {
        if (rule == null || rules.contains(rule)) {
            return false;
        }
        rules.add(rule);
        return true;
    }

    public synchronized boolean addRuleFromUserInput(String raw) {
        if (raw == null) {
            return false;
        }
        FilterRule rule = FilterRule.deserialize(raw);
        if (rule == null) {
            return false;
        }
        return addRule(rule);
    }

    public synchronized boolean removeSelected() {
        if (selectedIndex < 0 || selectedIndex >= rules.size()) {
            return false;
        }
        rules.remove(selectedIndex);
        if (rules.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= rules.size()) {
            selectedIndex = rules.size() - 1;
        }
        return true;
    }
}
