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

package com.ensarsarajcic.neovim.java.notifications.ui.grid.linegrid;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class DefaultColorsSet implements UiLineGridEvent {
    public static final String NAME = "default_colors_set";

    private int fgColor;
    private int bgColor;
    private int specialColor;
    private int fgCtermColor;
    private int bgCtermColor;

    public DefaultColorsSet(
            @JsonProperty(value = "rgb_fg", index = 0) int fgColor,
            @JsonProperty(value = "rgb_bg", index = 1) int bgColor,
            @JsonProperty(value = "rgb_sp", index = 2) int specialColor,
            @JsonProperty(value = "cterm_fg", index = 3) int fgCtermColor,
            @JsonProperty(value = "cterm_bg", index = 4) int bgCtermColor) {
        this.fgColor=fgColor;
        this.bgColor=bgColor;
        this.specialColor=specialColor;
        this.fgCtermColor=fgCtermColor;
        this.bgCtermColor=bgCtermColor;
    }

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "GridClearEvent{" +
                "fgColor=" + fgColor +
                ", bgColor=" + bgColor +
                ", specialColor=" + specialColor +
                ", fgCtermColor=" + fgCtermColor +
                ", bgCtermColor=" + bgCtermColor +
                '}';
    }
}
