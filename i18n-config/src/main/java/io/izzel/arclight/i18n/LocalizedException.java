package io.izzel.arclight.i18n;

@SuppressWarnings("unchecked")
public interface LocalizedException {

    String node();

    Object[] args();

    static <T extends Exception & LocalizedException> T checked(String node, Object... args) {
        class Checked extends Exception implements LocalizedException {

            @Override
            public String node() {
                return node;
            }

            @Override
            public Object[] args() {
                return args;
            }
        }
        return (T) new Checked();
    }

    static <T extends RuntimeException & LocalizedException> T unchecked(String node, Object... args) {
        class Unchecked extends RuntimeException implements LocalizedException {

            @Override
            public String node() {
                return node;
            }

            @Override
            public Object[] args() {
                return args;
            }
        }
        return (T) new Unchecked();
    }
}
