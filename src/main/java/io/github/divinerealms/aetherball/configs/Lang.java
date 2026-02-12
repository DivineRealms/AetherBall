package io.github.divinerealms.aetherball.configs;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public enum Lang {
  INFO("prefix.info", "&8▎ &r"),
  SUCCESS("prefix.success", "&a▎ &r"),
  ERROR("prefix.error", "&4▎ &c"),
  WARN("prefix.warn", "&6▎ &e"),
  OTHER("prefix.other", "&b▎ &r"),
  DEBUG("prefix.debug", "&9▎ &e[&oDEBUG&e] &f"),

  BANNER_PLAYER("banner.player", String.join(System.lineSeparator(),
      "&9        __",
      "&3     .&9'&f\"'..\"&9'&3.      &e&l{0}&7, version: &6{1}",
      "&b    :.&f_.\"\"._&b.:     &7Authors: &d&l{2}",
      "&3    : &r &f\\_/&3 &3 :",
      "&b     '.&f/  \\&b.'      &9&nhttps://github.com/DivineRealms/AetherBall",
      "&9        \"\"",
      "&r &r",
      "&aType &e/fc help &afor a list of available commands.")),

  NO_PERM("no-perm.single", "{prefix_error}Insufficient permissions."),
  NO_PERM_PARAMETERS("no-perm.parameters",
      "{prefix_error}You don't have permission for command &6/&e{1}&c!"),
  UNKNOWN_COMMAND("unknown-command", "{prefix_error}Unknown command."),
  USAGE("usage", "{prefix_warn}Usage: /{0}"),
  PLAYER_NOT_FOUND("player-not-found", "{prefix_error}Player not found."),
  ON("toggle.on", "&aON"),
  OFF("toggle.off", "&cOFF"),

  RELOAD("reload", "{prefix_success}AetherBall reloaded!"),
  FC_TOGGLE("toggle.status", "{prefix_success}AetherBall Matches turned {0}"),
  ADMIN_STATSET("statset", "{prefix_success}Updated {0} for player {1} to {2}"),

  MATCHMAN_FORCE_START("matchman.force-start", "{prefix_success}Started {0} match."),
  MATCHMAN_FORCE_END("matchman.force-end", "{prefix_success}Ended {0} match."),

  FORCE_LEAVE("force-leave", "{prefix_success}Removed player {0}&f from all matches/lobbies."),

  HITDEBUG_PLAYER_WHOLE("debug.hits-player.whole", "{0}{1}"),
  HITDEBUG_PLAYER_CHARGED("debug.hits-player.charged",
      "&8[&dCharged&8] &a{0}KP &8[&e&o{1}PW&8, &d&o{2}CH&8]"),
  HITDEBUG_PLAYER_REGULAR("debug.hits-player.regular", "&8[&aRegular&8] &a{0}KP"),
  HITDEBUG_PLAYER_COOLDOWN("debug.hits-player.cooldown", "&8 [&fCD: {0}{1}ms&8]"),
  HITDEBUG_WHOLE("debug.hits.whole", "{0}"),
  HITDEBUG_CHARGED("debug.hits.charged",
      "&dCharged &8| {0} &8| {1}KP &8| &e{2}PW&7, &d{3}CH"),
  HITDEBUG_REGULAR("debug.hits.regular", "&aRegular &8| {0} &8| &a{1}KP"),
  HITDEBUG_VELOCITY_CAP("debug.hits.velocity-cap",
      "{prefix_debug}Velocity Cap Triggered! &7Speed: &e{0} &7-> &a{1} &7| Hitter: &f{2}"),

  JOIN_SUCCESS("join.success",
      "{prefix_success}&b[Coach] You've joined the {0} lobby. Wait for players to gather..."),
  JOIN_INVALIDTYPE("join.invalid-arena-type", "{prefix_error}{0} is not a valid arena type"),
  JOIN_ALREADYINGAME("join.already-in-game", "{prefix_error}You are already in a game"),
  JOIN_NOARENA("join.no-arena", "{prefix_error}No arena found."),

  TEAM_NO_REQUEST("team.no-request", "{prefix_error}There is no team request to accept"),
  TEAM_ACCEPT_OTHER("team.accept.other",
      String.join(System.lineSeparator(), "{prefix_success}You successfully teamed with {0}",
          "{prefix_info}You must wait for a team slot to open, this won't take long")),
  TEAM_ACCEPT_SELF("team.accept.self",
      String.join(System.lineSeparator(), "{prefix_success}You successfully teamed with {0}",
          "{prefix_info}You must wait for a team slot to open, this won't take long")),
  TEAM_DECLINE_OTHER("team.decline.other", "{prefix_error}{0} declined your team request"),
  TEAM_DECLINE_SELF("team.decline.self",
      "{prefix_success}You successfully declined the team request"),
  TEAM_ALREADY_IN_TEAM("team.already-in-team", "{prefix_error}You're already in a team"),
  TEAM_ALREADY_IN_TEAM_2("team.already-in-team-2", "{prefix_error}{0} is already in a team"),
  TEAM_WANTS_TO_TEAM_OTHER("team.wants-to-team.other",
      String.join(System.lineSeparator(),
          "{prefix_info}&b{0} &fwants to team with you for a &e{1}v{1} match",
          "{prefix_info}&e/fc team accept &for &e/fc team decline &fto respond to the team request")),
  TEAM_WANTS_TO_TEAM_SELF("team.wants-to-team.self",
      String.join(System.lineSeparator(),
          "{prefix_info}You successfully sent &b{0} &fa team request for a &e{1}v{1} match",
          "{prefix_info}&e/fc team cancel &fto cancel this")),
  TEAM_ALREADY_IN_GAME("team.already-in-game", "{prefix_error}{0} is already in a game"),
  TEAM_NOT_ONLINE("team.not-online", "{prefix_error}{0} is not online"),
  TEAM_DISBANDED("team.disbanded", "{prefix_error}Your team was disbanded because {0} left."),
  TEAM_INVITE_EXPIRED("team.invite-expired", "{prefix_error}Team request has expired."),

  TAKEPLACE_SUCCESS("takeplace.success",
      "{prefix_success}&b[Coach] You've taken a spot in match #{0}"),
  TAKEPLACE_INGAME("takeplace.already-ingame", "{prefix_error}You are already in a match"),
  TAKEPLACE_NOPLACE("takeplace.no-place", "{prefix_error}There is no available spot"),
  TAKEPLACE_AVAILABLE_HEADER("takeplace.available.header",
      "{prefix_info}Available matches to join:"),
  TAKEPLACE_AVAILABLE_ENTRY("takeplace.available.entry",
      "{prefix_info}&e- Match #{0} &7({1}) - &a{2} &7slots open"),
  TAKEPLACE_INVALID_ID("takeplace.invalid-id",
      "{prefix_error}Could not find an open match with ID #{0}."),
  TAKEPLACE_FULL("takeplace.full", "{prefix_error}Match #{0} is already full."),

  STATS_NONE("stats.none", "{prefix_error}{0} has never played AetherBall"),
  STATS("stats.info", String.join(System.lineSeparator(),
      "&e---------------------------------------------",
      "&r &6{0} statistics:",
      "&r &r",
      "&7 Matches played: &f{1}",
      "&7 Match record: &a{2}W &c{3}L &9{4}T",
      "&7 Win rate: &2{5}",
      "&7 Best winstreak: &e{6}",
      "&7 Goals scored: &a{7}",
      "&7 Goals per match: &a{8}",
      "&7 Skill level: &a{9} &7| Rank: &f{10}",
      "&r &r",
      "&7&o Use &6/&efc stats &2<&aplayer-name&2> &7&ofor others...",
      "&e---------------------------------------------")),

  LEAVE_NOT_INGAME("leave.not-ingame", "{prefix_error}You are not even in a match"),
  LEFT("leave.left", "{prefix_success}You left the match."),
  LEAVE_LOSING("leave.losing",
      "{prefix_error}You left while losing! You were fined &e${0} &cand cannot join for &e{1} minutes&c."),

  UNDO("undo", "{prefix_success}Undo successful"),

  CLEAR_STATS_SUCCESS("clear-stats.success", "{prefix_success}You successfully wiped {0}'s stats"),

  STATSSET_IS_NOT_A_NUMBER("stats-set.is-not-a-number", "{prefix_error}{0} is not a number"),
  STATSSET_NOT_A_STAT("stats-set.not-a-stat",
      String.join(System.lineSeparator(), "{prefix_error}{0} is not a stat, choose from:",
          "{prefix_info}&7wins, matches, ties, goals, streak, store, all")),

  SETUP_ARENA_START("setup-arena.start",
      String.join(System.lineSeparator(), "{prefix_info}You just started setting up an arena",
          "{prefix_info}If you got here by accident, use &e/fc undo",
          "{prefix_info}&7Step 1: &fStand in the center block behind the blue goal line facing the &cred &fgoal, then use &e/fc set")),
  SETUP_ARENA_FIRST_SET("setup-arena.first-set",
      String.join(System.lineSeparator(), "{prefix_success}First location successfully set.",
          "{prefix_info}Now do the same for the other goal")),
  SETUP_ARENA_SUCCESS("setup-arena.success", "{prefix_success}You successfully set up the arena"),

  CLEAR_ARENAS_SUCCESS("clear-arenas.success", "{prefix_success}You successfully wiped all arenas"),
  CLEAR_ARENAS_TYPE_SUCCESS("clear-arenas.type-success",
      "{prefix_success}Successfully cleared &e{0} &farenas!"),
  ALREADY_ENOUGH_CUBES("already-enough-cubes", "{prefix_error}There are already enough cubes"),

  BALANCE("balance", "{prefix_info}You currently have &e#{0}"),

  TAKE_PLACE_ANNOUNCEMENT_LOBBY("match.tkp-announcement.lobby",
      "{prefix_info}&6&lSUBSTITUTION: &fSomeone left {0}&f during the discussion phase."),
  TAKE_PLACE_ANNOUNCEMENT_MATCH("match.tkp-announcement.match", String.join(System.lineSeparator(),
      "{prefix_warn}&6&lSUBSTITUTION: &fSomeone left {0}&f!",
      "{prefix_warn}&fScore: {1} &f{2} - {3} {4} &7(time remaining: &e{5}&7)",
      "{prefix_warn}&fType &e/tkp <id> &ato take their place!")),

  CUBE_SPAWN("cube.spawn", "{prefix_success}You spawned a cube"),
  CUBE_CLEAR("cube.clear", "{prefix_success}Cleared nearest cube"),
  CUBE_CLEAR_ALL("cube.clear-all", "{prefix_success}Cleared {0} cube{1}"),
  CUBE_NO_CUBES("cube.no-cube", "{prefix_error}No cubes near you"),

  COMMAND_DISABLER_ALREADY_ADDED("command-disabler.already-added",
      "{prefix_error}This command was already added"),
  COMMAND_DISABLER_SUCCESS("command-disabler.added-successfully",
      "{prefix_success}You successfully added command &e/{0} &fto the list of disabled commands"),
  COMMAND_DISABLER_SUCCESS_REMOVE("command-disabler.removed-successfully",
      "{prefix_success}You successfully removed command &e/{0} &ffrom the list of disabled commands"),
  COMMAND_DISABLER_WASNT_ADDED("command-disabler.wasnt-added",
      "{prefix_error}This command wasn't even added"),
  COMMAND_DISABLER_LIST("command-disabler.list", "{prefix_info}List of disabled commands:"),
  COMMAND_DISABLER_CANT_USE("command-disabler.cant-use",
      "{prefix_error}You cannot use this command during a match"),

  STARTING("match.starting", String.join(System.lineSeparator(),
      "{prefix_other}There are enough players to start, the match will begin in 30 seconds.",
      "{prefix_other}You now have time to discuss your strategy.",
      "{prefix_other}&3TIP: &fChoose someone to be goalkeeper.",
      "{prefix_other}Use &e/tc [Message] &ffor team chat.")),
  TEAMCHAT_RED("match.tc-red", "&cTC {0}&f: "),
  TEAMCHAT_BLUE("match.tc-blue", "&1TC {0}&f: "),
  MATCH_STARTED("match.started", "{prefix_other}The match has started, good luck"),
  MATCH_ALREADY_STARTED("match.already-started", "{prefix_error}Match has already started."),
  MATCH_PROCEED("match.proceed", "{prefix_other}The match will now proceed"),
  MATCH_TIMES_UP("match.times-up", "{prefix_other}Time's up! The {0} team has won"),
  MATCH_WIN_CREDITS("match.win-credits", "{prefix_other}You got 15 credits for winning"),
  MATCH_WINSTREAK_CREDITS("match.winstreak-credits",
      "{prefix_other}&6&lYou get 100 credits bonus for winning {0} times in a row!!!"),
  MATCH_TIED("match.tied", "{prefix_other}Time's up! The game is tied"),
  MATCH_TIED_CREDITS("match.tied-credits", "{prefix_other}You got 5 credits for a tie"),
  MATCH_SCORE_CREDITS("match.score-credits", "{prefix_other}You got 10 credits for scoring"),
  MATCH_CLEAN_SHEET_BONUS("match.clean-sheet-bonus",
      "{prefix_other}You got 30 credits for a clean sheet"),
  MATCH_ASSIST_CREDITS("match.assist-credits", "{prefix_other}You got 5 credits for an assist"),
  MATCH_SCORE_HATTRICK("match.score-hattrick",
      "{prefix_other}&6&lYou get 100 credits bonus for a hat-trick"),
  MATCH_HATTRICK("match.hattrick", "&6&lHAT-TRICK!!!"),
  MATCH_GOALLL("match.goalll", "&6&lGOOOOAL!!!"),
  MATCH_GOAL("match.goal", String.join(System.lineSeparator(),
      "{0} &a{1} scored a goal for the {2} team from {3} blocks away and got assisted by {4}")),
  MATCH_GOAL_ASSIST("match.goal-assist", "Assist: {0}"),
  MATCH_SCORE_STATS("match.score-stats",
      String.join(System.lineSeparator(), "&aIt is now {0}-{1} Red-Blue",
          "{prefix_other}The match will continue in 10 seconds")),
  MATCH_SCORE_OWN_GOAL_ANNOUNCE("match.score-own-goal-announce",
      "&6&lOWN GOAL! &a{0} scored a goal for the opposing team"),
  MATCH_PREVENT_ABUSE("match.prevent-abuse",
      "{prefix_error}Command abuse is prohibited during the match."),
  MATCH_TYPE_UNAVAILABLE("match.type-unavailable",
      "{prefix_error}&e{0} &cmatches are not available. Available: &a{1}"),
  LEAVE_QUEUE_ACTIONBAR("match.leave-queue-actionbar", "&cYou left the {0} queue..."),
  PLAYER_PLACEHOLDERS("match.player-placeholders", "%luckperms_prefix%%player_name%"),

  MATCH_STARTING_TITLE("match.starting-title", "&a&lGL HF!"),
  MATCH_STARTING_SUBTITLE("match.starting-subtitle", "&2The match has started!"),
  MATCH_PREPARATION_TITLE("match.preparation-title", "&e&lPreparing..."),
  MATCH_PREPARATION_SUBTITLE("match.preparation-subtitle", "&6Who will defend?"),
  MATCH_PREPARING("match.preparing", "&3Preparing match..."),
  MATCH_STARTING_ACTIONBAR("match.starting-actionbar", "{0} &8┃ &e{1}"),
  MATCH_STARTED_ACTIONBAR("match.started-actionbar", "Match started, good luck!"),

  QUEUE_ACTIONBAR("match.queue-actionbar", "{0} &8┃ {1} &7({2}&7/&a{3}&7)"),

  BEST_HEADER("best.header",
      String.join(System.lineSeparator(), "&6All AetherBall highscores:",
          "&bBest ratings:")),
  BEST_ENTRY("best.entry", "&7{0}. {1} - {2}"),
  BEST_GOALS("best.most-goals", "&bMost goals:"),
  BEST_ASSISTS("best.most-assists", "&bMost assists:"),
  BEST_OWN_GOALS("best.most-own-goals", "&bMost own goals:"),
  BEST_WINS("best.most-wins", "&bMost wins:"),
  BEST_WINSTREAK("best.winstreak", "&bLongest win streak:"),
  BEST_UPDATING("best.updating",
      "&cHighscores are not yet available. Try again in a few seconds..."),

  NOBODY("nobody", "nobody"),

  HELP_ADMIN("help.admin", String.join(System.lineSeparator(),
      "&r &r",
      "&c                    &lAetherBall Admin Commands!",
      "&r &r",
      "&6  ▪ &c&lArena Setup:",
      "&e  /fca setuparena &a<2v2|3v3|4v4>&7: &f&oStart arena creation wizard.",
      "&e  /fca set&7: &f&oSet spawn point (use twice per arena).",
      "&e  /fca undo&7: &f&oCancel current arena setup.",
      "&e  /fca clear arenas &3[2v2|3v3|4v4]&7: &f&oDelete all or specific arenas.",
      "&r &r",
      "&6  ▪ &c&lLocation Setup:",
      "&e  /fca setlobby&7: &f&oSet main lobby spawn location.",
      "&e  /fca setpracticearea &a<n>&7: &f&oDefine practice zone location.",
      "&e  /fca setbutton &a<spawn|clearcube>&7: &f&oCreate interactive button.",
      "&r &r",
      "&6  ▪ &c&lSystem Management:",
      "&e  /fca toggle&7: &f&oEnable/disable matchmaking system.",
      "&e  /fca reload configs&7: &f&oReload configuration files.",
      "&e  /fca reload arenas&7: &f&oReload arena data only.",
      "&e  /fca reload all&7: &f&oReload all systems and configs.",
      "&r &r",
      "&6  ▪ &c&lPlayer Bans:",
      "&e  /fca ban &a<player> <time>&7: &f&oBan player from matches.",
      "&e  /fca unban &a<player>&7: &f&oRemove player ban.",
      "&e  /fca checkban &a<player>&7: &f&oCheck player's ban status.",
      "&r &r",
      "&6  ▪ &c&lPlayer Management:",
      "&e  /fca statset &a<player> <stat> <value>&7: &f&oModify player stats.",
      "&e  /fca forceleave &a<player>&7: &f&oRemove player from match.",
      "&e  /fca refreshprefix &a<player>&7: &f&oUpdate cached prefix.",
      "&e  /fca clear stats&7: &f&oWipe all player statistics.",
      "&r &r",
      "&6  ▪ &c&lPerformance & Tasks:",
      "&e  /fca tasks&7: &f&oView task performance report.",
      "&e  /fca tasks restart&7: &f&oRestart all plugin tasks.",
      "&e  /fca tasks reset&7: &f&oReset task statistics.",
      "&r &r",
      "&6  ▪ &c&lDebug & Testing:",
      "&e  /fca hitsdebug&7: &f&oToggle global hit debug mode.",
      "&e  /fca commanddisabler add &a<cmd>&7: &f&oBlock command in matches.",
      "&e  /fca commanddisabler remove &a<cmd>&7: &f&oUnblock command.",
      "&e  /fca commanddisabler list&7: &f&oShow all blocked commands.",
      "&e  /matchman start&7: &f&oForce start current match.",
      "&e  /matchman end&7: &f&oForce end current match.",
      "&r &r",
      "&6  ▪ &c&lQuick Reference:",
      "&7  Stats: &fwins, matches, ties, goals, assists, owngoals,",
      "&7         winstreak, bestwinstreak",
      "&7  Time: &f10s, 5min, 1h, 30min (for bans)",
      "&7  Match types: &f2v2, 3v3, 4v4",
      "&r &r"
  )),
  HELP_HEADER("help.header", "&e-------------[ &6&lAetherBall Help &e]----------------"),
  HELP_FOOTER("help.footer", "&e---------------------------------------------"),
  HELP_USAGE("help.usage", "&cUsage: &e{command} &a{syntax}"),
  HELP_CATEGORIZED("help.categorized", String.join(System.lineSeparator(),
      "&e-------------[ &6&lAetherBall Help &e]----------------",
      "&r &r",
      "&a                 &lMain help page.",
      "&r &r",
      "&e  /fc help gameplay&7: &f&oJoin matches and play!",
      "&e  /fc help teams&7: &f&oTeam up with friends!",
      "&e  /fc help settings&7: &f&oCustomize your experience.",
      "&e  /fc help utility&7: &f&oSpawn cubes, stats, leaderboard...",
      "&r &r",
      "&7        &oType &e/fc help &a<category> &7&ofor more info.",
      "&e---------------------------------------------"
  )),
  HELP_CATEGORY_1("help.category.1", String.join(System.lineSeparator(),
      "&r &r",
      "&a                        &lGameplay commands!",
      "&r &r",
      "&6  ▪ &b&lJoin a match/queue:",
      "&e  /fcjoin &6or &e/fcj &a<2v2|3v3|4v4>",
      "&e  /fc join &6or &e/j &a<2v2|3v3|4v4>",
      "&e  /2v2 &6or &e/3v3 &6or &e/4v4",
      "&r &r",
      "&6  ▪ &b&lLeave a match/queue:",
      "&e  /fcleave &6or &e/fcl &6| &e/fc leave &6or &e/l &6| &e/leave",
      "&r &r",
      "&6  ▪ &b&lTake a place in an ongoing match:",
      "&e  /fc takeplace &6or &e/tkp &6| &e/takeplace &6or &e/tkp",
      "&r &r",
      "&6  ▪ &b&lOther commands:",
      "&e  /fc teamchat &6or &e/tc &a<msg>&7: &f&oSend message to team.",
      "&e  /matches &6or &e/q&7: &f&oView all active matches.",
      "&r &r"
  )),
  HELP_CATEGORY_2("help.category.2", String.join(System.lineSeparator(),
      "&r &r",
      "&a                             &lTeam commands!",
      "&r &r",
      "&e  /fc team &a<2v2|3v3|4v4> <player>&7: &f&oInvite player.",
      "&e  /fc team accept&7: &f&oAccept team invitation.",
      "&e  /fc team decline&7: &f&oDecline team invitation.",
      "&r &r",
      "&7       &oTeams automatically disband after matches."
  )),
  HELP_CATEGORY_3("help.category.3", String.join(System.lineSeparator(),
      "&r &r",
      "&a                   &lSettings commands!",
      "&r &r",
      "&6  ▪ &b&lToggles:",
      "&e  /fct &6or &e/fc toggle kick&7: &f&oToggle kick sound effects.",
      "&e  /fct &6or &e/fc toggle goal&7: &f&oToggle goal sound effects.",
      "&e  /fct &6or &e/fc toggle particles&7: &f&oToggle particle trails.",
      "&e  /fct &6or &e/fc toggle particlemode&7: &f&oToggle particle distance mode.",
      "&e  /fct &6or &e/fc toggle hits&7: &f&oToggle hit visualization.",
      "&r &r",
      "&6  ▪ &b&lCustomization:",
      "&e  /fcss &6or &e/fc setsound kick &a<sound>&7: &f&oSet kick sound.",
      "&e  /fcss &6or &e/fc setsound goal &a<sound>&7: &f&oSet goal sound.",
      "&e  /fcsp &6or &e/fc setparticle &a<type> &3[color]&7: &f&oSet trail.",
      "&e  /fcsgc &6or &e/fc setgoalcelebration &a<style>&7: &f&oSet goal style.",
      "&r &r",
      "&7     &oUse &alist &7&oas an argument to see all options."
  )),
  HELP_CATEGORY_4("help.category.4", String.join(System.lineSeparator(),
      "&r &r",
      "&a                            &lUtility commands!",
      "&r &r",
      "&e  /cube &6or &e/fc cube&7: &f&oSpawn a cube!",
      "&e  /clearcube &6or &e/fc clearcube&7: &f&oClear nearest cube.",
      "&e  /clearcubes &6or &e/fc clearcubes&7: &f&oClear all cubes.",
      "&e  /stats &6or &e/fc stats&7: &f&oView player statistics.",
      "&e  /highscores &6or &e/best &6or &e/fc highscores &6or &e/fc best",
      "&e  /build&7: &f&oActivate build mode.",
      "&r &r"
  )),
  HELP_CATEGORY_UNKNOWN("help.category.unknown", String.join(System.lineSeparator(),
      "&c                      &lUnknown category!",
      "&r &r",
      "&f                    List of available categories:",
      "&e                 gameplay, teams, settings, utility",
      "&r &r",
      " &7            &oUse &e/fc help|h &7for all categories."
  )),

  SCOREBOARD_LINES_LOBBY("scoreboard.lines.lobby", String.join(System.lineSeparator(),
      "&r &r",
      "&r &r",
      "&f &lPlayers:",
      "&f{0}",
      "&r &r",
      "&f {1}",
      "&r &r"
  )),
  SCOREBOARD_LINES_RED_PLAYERS_ENTRY("scoreboard.lines.red-players-entry", "&r  {0}. &c{1}"),
  SCOREBOARD_LINES_BLUE_PLAYERS_ENTRY("scoreboard.lines.blue-players-entry", "&r  {0}. &9{1}"),
  SCOREBOARD_LINES_WAITING_PLAYERS_ENTRY("scoreboard.lines.waiting-players-entry", "&r  {0}. {1}"),
  SCOREBOARD_LINES_MATCH("scoreboard.lines.match", String.join(System.lineSeparator(),
      "&r &r",
      "{0}",
      "&r &r",
      "&c &lRed &f&l{1} - {2} &9&lBlue",
      "&r &r",
      "&e &lTime left:",
      "&6  ┗ &e{3}",
      "&r &r"
  )),
  SCOREBOARD_FOOTER("scoreboard.footer", "&b   &ndivinerealms.net&r  "),

  RED("red", "&cRed"),
  BLUE("blue", "&9Blue"),
  INGAME_ONLY("ingame-only", "{prefix_error}In-game only command."),

  PRACTICE_AREAS_EMPTY("practice-areas-empty",
      "{prefix_error}No practice zones defined. Not clearing cubes."),

  CLEARED_CUBES("cleared-cubes", "&bℹ Cleared &e{0} cube(s) &ffrom practice zones &bℹ"),
  CLEARED_CUBE_INGAME("cleared-cube-ingame",
      "{prefix_other}Cube was cleared during the match! Respawning it."),

  PRACTICE_AREA_SET("practice-area-set",
      "{prefix_success}Successfully set practice zone &b{0}&f (&o{1}, {2}, {3}&f)."),

  FC_DISABLED("fc-disabled",
      "{prefix_error}Joining matches is temporarily disabled by an admin."),

  TOGGLES_KICK("toggles.kick", "{prefix_success}Kick sound is {0}&f!"),
  TOGGLES_GOAL("toggles.goal", "{prefix_success}Goal sound is {0}&f!"),
  TOGGLES_PARTICLES("toggles.particles", "{prefix_success}Particle trails are {0}&f!"),
  TOGGLES_PARTICLES_MODE("toggles.particles-mode",
      "{prefix_success}Particle trail mode set to: &a{0}"),
  TOGGLES_HIT_DEBUG("toggles.hit-debug", "{prefix_success}Hit debug is {0}&f!"),

  INVALID_TYPE("type.invalid", "{prefix_error}That &e{0} &fcannot be used."),
  INVALID_COLOR("type.invalid-color", "{prefix_error}Color &e{0} &fcannot be used."),
  AVAILABLE_TYPE("type.available", "{prefix_info}Available &e{0}&f: &e{1}"),
  SOUND("type.sound", "sound"),
  PARTICLE("type.particle", "particle"),
  COLOR("type.color", "color"),

  SET_SOUND_KICK("set.sound.kick", "{prefix_success}Kick sound set to: &e{0}"),
  SET_SOUND_GOAL("set.sound.goal", "{prefix_success}Goal sound set to: &e{0}"),
  SET_PARTICLE("set.particle.regular", "{prefix_success}Particle set to: &e{0}"),
  SET_PARTICLE_REDSTONE("set.particle.redstone",
      "{prefix_success}Particle set to &e{0} &fwith color {1}"),
  SET_BUILD_MODE("set.build-mode.self", "{prefix_success}Build mode {0}&f!"),
  SET_BUILD_MODE_OTHER("set.build-mode.other", "{prefix_success}Build mode for &b{0}&f is {1}&f!"),
  SET_GOAL_CELEBRATION("set.goal-celebration",
      "{prefix_success}You set your goal celebration style to: &e{0}"),
  GM_EPIC_TITLE_1("goal-messages.epic.title-1", "&c&lOWN GOAL!"),
  GM_EPIC_TITLE_1_GOAL("goal-messages.epic.title-1-goal", "&e&lGOOOOAL!"),
  GM_EPIC_TITLE_1_HATTY("goal-messages.epic.title-1-hattrick", "&6&l⚡ HAT-TRICK! ⚡"),
  GM_EPIC_SUBTITLE_1("goal-messages.epic.subtitle-1-own", "&7Oh no..."),
  GM_EPIC_SUBTITLE_1_SCORER("goal-messages.epic.subtitle-1-scorer", "&a&lYOU SCORED!"),
  GM_EPIC_SUBTITLE_1_OTHER("goal-messages.epic.subtitle-1-other", "&7{0}"),
  GM_EPIC_TITLE_2("goal-messages.epic.title-2", "&c{0}"),
  GM_EPIC_TITLE_2_GOAL("goal-messages.epic.title-2-goal", "&6{0}"),
  GM_EPIC_SUBTITLE_2("goal-messages.epic.subtitle-2", "&7Scorer: &f{0}{1}"),
  GM_EPIC_TITLE_3("goal-messages.epic.title-3", "&e{0} blocks"),
  GM_EPIC_SUBTITLE_3("goal-messages.epic.subtitle-3", "{0} &f{1} - {2} {3}"),
  GM_SIMPLE_TITLE("goal-messages.simple.title", "&c&lOWN GOAL!"),
  GM_SIMPLE_TITLE_GOAL("goal-messages.simple.title-goal", "&e&lGOOOOAL!"),
  GM_SIMPLE_SUBTITLE("goal-messages.simple.subtitle", "&7{0} &ffrom &e{1} blocks"),
  GM_MINIMAL_OWN("goal-messages.minimal.own", "&cOWN GOAL &8┃ {0} &8┃ {1} &f{2} - {3} {4}"),
  GM_MINIMAL_GOAL("goal-messages.minimal.goal", "&eGOOOOAL &8┃ {0} &8┃ {1} &f{2} - {3} {4}"),
  GM_DEFAULT_TITLE_OWN("goal-messages.default.title-own", "&c&lOWN GOAL!"),
  GM_DEFAULT_TITLE_GOAL("goal-messages.default.title-goal", "&e&lGOOOOAL!"),
  GM_DEFAULT_TITLE_SCORER("goal-messages.default.title-scorer", "&a&lYOU SCORED!"),
  GM_DEFAULT_TITLE_HATTY("goal-messages.default.title-hattrick", "&6&lHAT-TRICK!"),
  GM_DEFAULT_SUBTITLE_OWN("goal-messages.default.subtitle-own", "&7{0} &8→ &c{1}"),
  GM_DEFAULT_SUBTITLE_GOAL("goal-messages.default.subtitle-goal", "&7{0} &8┃ &e{1} blocks{2}"),
  GM_DEFAULT_ACTIONBAR("goal-messages.default.actionbar", "{0} &f{1} - {2} {3}"),
  GM_ASSISTS_TEXT("goal-messages.assists-text", "&7 (&f{0}&7)"),

  SET_BLOCK_TOO_FAR("set.block.too-far",
      "{prefix_error}You must be looking at a block within 5 blocks of you."),
  SET_BLOCK_SUCCESS("set.block.success", "{prefix_success}Successfully set &e{0} &fbutton!"),

  BLOCK_INTERACT_COOLDOWN("block-interact-cooldown",
      "{prefix_error}Wait &e{0} &cbefore using this again."),

  MATCHES_LIST_NO_MATCHES("matches.list.no-matches",
      "{prefix_error}There are currently no active matches..."),
  MATCHES_LIST_WAITING("matches.list.waiting", "&8&oWaiting for players..."),
  MATCHES_LIST_STARTING("matches.list.starting", "&eStarting in &c{0}s"),
  MATCHES_LIST_LOBBY("matches.list.lobby", "&6 &l[{0} Lobby {1}]"),
  MATCHES_LIST_MATCH("matches.list.match", "&a &l[{0} Match {1}]"),
  MATCHES_LIST_STATUS("matches.list.status", "&7 Status: &r{0}"),
  MATCHES_LIST_RESULT("matches.list.result", "&7 Score: &c{0} &7- &9{1}"),
  MATCHES_LIST_TIMELEFT("matches.list.timeleft", "&7Time remaining: &e{0}"),
  MATCHES_LIST_REDPLAYERS("matches.list.players.red", "&c Red&7: &r{0}"),
  MATCHES_LIST_BLUEPLAYERS("matches.list.players.blue", "&9 Blue&7: &r{0}"),
  MATCHES_LIST_WAITINGPLAYERS("matches.list.players.waiting", "&f Queue&7: &r{0}"),
  MATCHES_LIST_HEADER("matches.list.header", String.join(System.lineSeparator(),
      "&e---------------------------------------------",
      "&r &eList of active matches:",
      "&r &r")),
  MATCHES_LIST_FOOTER("matches.list.footer", String.join(System.lineSeparator(),
      "&r &r",
      "&7&o Use &6/&efc join &2<&a2v2&2|&a3v3&2|&a4v4&2> &7&oto join...",
      "&e---------------------------------------------")),

  TASKS_REPORT_HEADER("plugin-stats.report.tasks.header", String.join(System.lineSeparator(),
      "&e-------------[ &6&lTask Status &e]----------------",
      "&r &r",
      "&7  Running: &e{0} &8/ &7{1}",
      "&r &r")),
  TASKS_REPORT_ENTRY("plugin-stats.report.tasks.entry", "  {0} &d{1}&f: &a{2}ms &87({3} runs)"),
  TASKS_REPORT_FOOTER("plugin-stats.report.tasks.footer", String.join(System.lineSeparator(),
      "&r &r",
      "&7 &lOverall Average Tick Time: &a{0}ms",
      "&e---------------------------------------------")),
  TASKS_RESTART("plugin-stats.report.tasks.restart",
      "{prefix_success}All tasks have been restarted."),
  TASKS_RESET_STATS("plugin-stats.report.tasks.reset-stats",
      "{prefix_success}All task statistics have been reset."),

  PLAYER_BANNED("bans.success", "{prefix_success}&b{0} &fhas been banned from FC for &e{1}&f."),
  PLAYER_UNBANNED("bans.unbanned", "{prefix_success}&b{0} &fhas been unbanned."),
  BAN_REMAINING("bans.remaining", "{prefix_info}&b{0} &fis banned for another &e{1}&f."),
  NOT_BANNED("bans.not-banned", "{prefix_error}&b{0} &cis not banned."),

  SIMPLE_FOOTER("simple-footer", "&e---------------------------------------------");

  private static FileConfiguration LANG;
  private final String path;
  private final String def;

  private static final Map<String, String> CONSOLE_PREFIX_CACHE = new HashMap<>();
  private static final Map<String, String> PLAYER_PREFIX_CACHE = new HashMap<>();

  Lang(String path, String start) {
    this.path = path;
    this.def = start;
  }

  public static void setFile(FileConfiguration config) {
    LANG = config;
    rebuildPrefixCache();
  }

  /**
   * Rebuilds the prefix caches for both console and player output.
   * <p>
   * Should be called automatically when language configuration changes via setFile().
   * </p>
   */
  public static void rebuildPrefixCache() {
    CONSOLE_PREFIX_CACHE.clear();
    PLAYER_PREFIX_CACHE.clear();

    for (Lang prefix : new Lang[]{INFO, SUCCESS, ERROR, WARN, OTHER, DEBUG}) {
      String tag = "{prefix_" + prefix.name().toLowerCase() + "}";
      CONSOLE_PREFIX_CACHE.put(tag, "[" + prefix.name() + "] ");
      PLAYER_PREFIX_CACHE.put(tag, getColoredPrefix(prefix));
    }
  }

  /**
   * Gets the colored prefix string for a prefix Lang constant.
   *
   * @param prefixLang the prefix Lang constant (INFO, SUCCESS, ERROR, etc.)
   * @return the colored prefix string
   */
  private static String getColoredPrefix(Lang prefixLang) {
    if (LANG == null) {
      return color(prefixLang.def);
    }

    String prefix = LANG.getString(prefixLang.path, prefixLang.def);
    return color(prefix);
  }

  /**
   * Replaces placeholders in the Lang message with provided arguments.
   * <p>
   * Automatically handles {prefix_type} placeholders using player-friendly colored prefixes. For
   * console output, use {@link #replace(boolean, String...)} with forConsole=true.
   * </p>
   *
   * @param args optional arguments for {0}, {1}, etc. placeholder replacement
   * @return the formatted and colored message string with player prefixes
   */
  public String replace(String... args) {
    return replace(false, args);
  }

  /**
   * Replaces placeholders in the Lang message with provided arguments and handles prefix
   * substitution based on output context.
   *
   * @param forConsole if true, uses [TYPE] format; if false, uses colored player prefixes
   * @param args       optional arguments for {0}, {1}, etc. placeholder replacement
   * @return the formatted and colored message string
   */
  public String replace(boolean forConsole, String... args) {
    if (LANG == null) {
      String message = replacePrefixes(def, forConsole);
      return color(message);
    }

    String value = LANG.getString(this.path, this.def);

    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if (args[i] != null) {
          value = value.replace("{" + i + "}", args[i]);
        }
      }
    }

    value = replacePrefixes(value, forConsole);
    return color(value);
  }

  /**
   * Replaces all {prefix_type} placeholders in a message.
   *
   * @param message    the message containing prefix placeholders
   * @param forConsole if true, uses [TYPE] format; if false, uses colored prefixes
   * @return message with prefix placeholders replaced
   */
  public static String replacePrefixes(String message, boolean forConsole) {
    if (message == null) {
      return "";
    }

    String result = message;
    Map<String, String> cache = forConsole ? CONSOLE_PREFIX_CACHE : PLAYER_PREFIX_CACHE;

    for (Map.Entry<String, String> entry : cache.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }

    return result;
  }

  /**
   * Translates alternate color codes in the given message using the '&' character.
   *
   * @param message the string containing alternate color codes
   * @return the formatted string with color codes translated
   */
  public static String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
  }

  @Override
  public String toString() {
    return this.replace();
  }
}