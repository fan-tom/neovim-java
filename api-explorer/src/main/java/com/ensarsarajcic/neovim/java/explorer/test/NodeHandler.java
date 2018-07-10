/*
 * MIT License
 *
 * Copyright (c) 2018 Ensar Sarajčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ensarsarajcic.neovim.java.explorer.test;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public final class NodeHandler {

    private NodeHandler() {
        throw new AssertionError("No instances");
    }

    public static Node generateNodeForType(String type) {
        switch (type) {
            case "Boolean":
                return generateBooleanNode();
            case "Integer":
                return generateIntegerNode();
            case "String":
                return new TextField();
            case "Object":
            case "Dictionary":
                return new TextField("JSON HERE");
            case "Array":
                return new TextField("Array here");
            case "Window":
                return new Label("NOT IMPLEMENTED YET (Window)");
            case "Tabpage":
                return new Label("NOT IMPLEMENTED YET (Tabpage)");
            case "Buffer":
                return new Label("NOT IMPLEMENTED YET (Buffer)");
        }

        if (type.startsWith("Array")) {
            return new TextField("Array here");
        }

        return new TextField(type);
    }

    private static Node generateBooleanNode() {
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton trueButton = new RadioButton("True");
        trueButton.setSelected(true);
        RadioButton falseButton = new RadioButton("False");
        trueButton.setToggleGroup(toggleGroup);
        falseButton.setToggleGroup(toggleGroup);
        return new HBox(trueButton, falseButton);
    }

    private static Node generateIntegerNode() {
        return new NumberField();
    }

    public static Object generateValueFromNodeOfType(Node node, String type) {
        return new Object();
    }
}
