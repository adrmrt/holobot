package dev.zawarudo.holo.modules.pokemon.utils;

import dev.zawarudo.holo.modules.pokemon.model.EvolutionChain;
import dev.zawarudo.holo.utils.Formatter;
import org.jetbrains.annotations.NotNull;

public final class EvolutionChainFormatter {

    private EvolutionChainFormatter() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String format(@NotNull EvolutionChain evolution, @NotNull String speciesName) {
        StringBuilder sb = new StringBuilder();

        // No evolutions
        if (evolution.getChain().getEvolvesTo().isEmpty()) {
            sb.append(Formatter.formatPokemonName(speciesName));
        }

        // One evolution
        else if (evolution.getChain().getEvolvesTo().getFirst().getEvolvesTo().isEmpty()) {
            String stage1 = Formatter.formatPokemonName(evolution.getChain().getSpecies().getName());
            sb.append(stage1)
                .append(" → ")
                .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getSpecies().getName()));

            for (int i = 1; i < evolution.getChain().getEvolvesTo().size(); i++) {
                sb.append("\n")
                    .append(stage1)
                    .append(" → ")
                    .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().get(i).getSpecies().getName()));
            }
        }

        // Two evolutions
        else {
            String stage1 = Formatter.formatPokemonName(evolution.getChain().getSpecies().getName());

            // Multiple stage 2 evolution
            if (evolution.getChain().getEvolvesTo().size() > 1) {
                sb.append(stage1)
                    .append(" → ")
                    .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getSpecies().getName()))
                    .append(" → ")
                    .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getEvolvesTo().getFirst().getSpecies().getName()));

                for (int i = 1; i < evolution.getChain().getEvolvesTo().size(); i++) {
                    sb.append("\n")
                        .append(stage1)
                        .append(" → ")
                        .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().get(i).getSpecies().getName()))
                        .append(" → ")
                        .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().get(i).getEvolvesTo().getFirst().getSpecies().getName()));
                }
            }

            // Only one stage 2 evolution
            else {
                String stage2 = Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getSpecies().getName());
                sb.append(stage1)
                    .append(" → ")
                    .append(stage2)
                    .append(" → ")
                    .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getEvolvesTo().getFirst().getSpecies().getName()));

                for (int i = 1; i < evolution.getChain().getEvolvesTo().getFirst().getEvolvesTo().size(); i++) {
                    sb.append("\n")
                        .append(stage1)
                        .append(" → ")
                        .append(stage2)
                        .append(" → ")
                        .append(Formatter.formatPokemonName(evolution.getChain().getEvolvesTo().getFirst().getEvolvesTo().get(i).getSpecies().getName()));
                }
            }
        }

        return sb.toString();
    }
}
