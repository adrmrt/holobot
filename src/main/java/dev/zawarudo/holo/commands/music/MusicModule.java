package dev.zawarudo.holo.commands.music;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import dev.zawarudo.holo.commands.CommandManager;
import dev.zawarudo.holo.commands.CommandModule;

public class MusicModule implements CommandModule {

    private final EventWaiter eventWaiter;

    public MusicModule(EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;
    }

    @Override
    public ModuleId id() {
        return ModuleId.MUSIC;
    }

    @Override
    public String description() {
        return "Commands related to music and the music player.";
    }

    @Override
    public void register(CommandManager registry) {
        ModuleId moduleId = id();

        registry.addCommand(new ClearCmd(eventWaiter), moduleId);
        registry.addCommand(new CloneCmd(), moduleId);
        registry.addCommand(new JoinCmd(), moduleId);
        registry.addCommand(new LeaveCmd(), moduleId);
        registry.addCommand(new LoopCmd(), moduleId);
        registry.addCommand(new NowPlayingCmd(), moduleId);
        registry.addCommand(new PlayCmd(), moduleId);
        registry.addCommand(new QueueCmd(), moduleId);
        registry.addCommand(new ShuffleCmd(), moduleId);
        registry.addCommand(new SkipCmd(eventWaiter), moduleId);
        registry.addCommand(new StopCmd(), moduleId);
    }
}