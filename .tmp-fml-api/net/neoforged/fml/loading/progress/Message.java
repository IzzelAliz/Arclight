/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.progress;

public class Message {
    private final String text;
    private final MessageType type;
    private final long timestamp;

    public Message(String text, MessageType type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.nanoTime();
    }

    public String getText() {
        return text;
    }

    MessageType getType() {
        return type;
    }

    long timestamp() {
        return timestamp;
    }

    public float[] getTypeColour() {
        return type.colour();
    }

    public enum MessageType {
        MC(1.0f, 1.0f, 1.0f),
        ML(0.0f, 0.0f, 0.5f),
        LOC(0.0f, 0.5f, 0.0f),
        MOD(0.5f, 0.0f, 0.0f);

        private final float[] colour;

        MessageType(float r, float g, float b) {
            colour = new float[] { r, g, b };
        }

        public float[] colour() {
            return colour;
        }
    }
}
