package dev.zawarudo.holo.utils.annotations;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.misc.EmbedColor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to pass information about a command
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {
    /**
     * The name of the command.
     */
    String name();

    /**
     * The description of the command.
     */
    String description();

    /**
     * The usage of the command.
     */
    String usage() default "";

    /**
     * An example of how to use the command.
     */
    String example() default "";

    /**
     * The aliases of the command.
     */
    String[] alias() default {};

    /**
     * The thumbnail of the command.
     */
    String thumbnail() default "";

    /**
     * The color of the embeds.
     */
    EmbedColor embedColor() default EmbedColor.DEFAULT;

    /**
     * The category of the command.
     */
    CommandCategory category() default CommandCategory.BLANK;

    /**
     * Whether the command can only be used in a guild. True by default.
     */
    boolean guildOnly() default true;

    /**
     * Whether the command can only be used by the guild admin. False by default.
     */
    boolean adminOnly() default false;

    /**
     * Whether the command can only be used by the bot owner. False by default.
     */
    boolean ownerOnly() default false;

    /**
     * Whether the command is NSFW (not safe for work). False by default.
     */
    boolean isNSFW() default false;
}
