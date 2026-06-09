package dev.zawarudo.holo.commands;

import java.util.Optional;

public interface CommandModule {

    ModuleId id();

    String description();

    void register(CommandManager registry);

    enum ModuleId {
        MUSIC("music"),
        POKEMON("pokemon");

        private final String id;

        ModuleId(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Optional<ModuleId> fromId(String id) {
            for (ModuleId m : values()) {
                if (m.id.equalsIgnoreCase(id)) return Optional.of(m);
            }
            return Optional.empty();
        }
    }
}
