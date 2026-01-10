package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.config.FilterRule;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterRulesEditorTest {

    @Test
    public void addRuleFromUserInput_acceptsVanillaSyntax() {
        FilterRulesEditor editor = new FilterRulesEditor();
        assertTrue(editor.addRuleFromUserInput("minecraft:stone@0"));
        assertEquals(1, editor.getRules().size());
        assertEquals(FilterRule.deserialize("minecraft:stone:0:0"), editor.getRules().get(0));
    }

    @Test
    public void addRuleFromUserInput_rejectsInvalidOrDuplicate() {
        FilterRulesEditor editor = new FilterRulesEditor();
        assertFalse(editor.addRuleFromUserInput(""));
        assertFalse(editor.addRuleFromUserInput(":::::"));
        assertTrue(editor.addRuleFromUserInput("minecraft:stone"));
        assertFalse(editor.addRuleFromUserInput("minecraft:stone"));
        assertEquals(1, editor.getRules().size());
    }

    @Test
    public void removeSelected_removesAndClampsSelection() {
        FilterRulesEditor editor = new FilterRulesEditor();
        editor.replaceAll(Arrays.asList(
                FilterRule.deserialize("minecraft:stone@0"),
                FilterRule.deserialize("minecraft:dirt@0")
        ));
        editor.setSelectedIndex(1);
        assertTrue(editor.removeSelected());
        assertEquals(1, editor.getRules().size());
        assertEquals(0, editor.getSelectedIndex());
    }
}

