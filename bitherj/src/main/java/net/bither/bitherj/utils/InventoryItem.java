/**
 * Copyright 2011 Google Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.bitherj.utils;

import java.util.Arrays;

public class InventoryItem {

    /**
     * 4 byte uint32 type field + 32 byte hash
     */
    public static final int MESSAGE_LENGTH = 36;

    public enum Type {
        Error(0x0),
        Transaction(0x1),
        Block(0x2),
        FilteredBlock(0x3),
        WitnessTransaction(0x40000001),
        WitnessBlock(0x40000002),
        WitnessFilteredBlock(0x40000003);

        public final int code;

        Type(int code) {
            this.code = code;
        }

        public static Type ofCode(int code) {
            for (Type type : values())
                if (type.code == code)
                    return type;
            return null;
        }
    }

    public final Type type;
    public final byte[] hash;

    public InventoryItem(Type type, byte[] hash) {
        this.type = type;
        this.hash = hash;
    }


    public String toString() {
        return type.toString() + ": " + hash;
    }

    public int hashCode() {
        return hash.hashCode() + type.ordinal();
    }

    public boolean equals(Object o) {
        return o instanceof InventoryItem &&
                ((InventoryItem) o).type == this.type && Arrays.equals(((InventoryItem) o).hash,
                this.hash);
    }
}
