package ru.breezeproject.api.analytics;

public final class AnalyticsEvents {

    public static final String PLAYER_JOIN = "player_join";
    public static final String PLAYER_QUIT = "player_quit";
    public static final String PLAYER_LOGIN = "player_login";
    public static final String PLAYER_DEATH = "player_death";
    public static final String PLAYER_KILL = "player_kill";
    public static final String MOB_KILL = "mob_kill";
    public static final String PLAYER_CHAT = "player_chat";

    public static final String PLAYER_COMMAND = "player_command";
    public static final String PLAYER_WORLD_CHANGE = "player_world_change";
    public static final String PLAYER_TELEPORT = "player_teleport";
    public static final String PLAYER_GAMEMODE_CHANGE = "player_gamemode_change";
    public static final String PLAYER_RESPAWN = "player_respawn";
    public static final String PLAYER_KICK = "player_kick";

    public static final String BLOCK_BREAK = "block_break";
    public static final String BLOCK_PLACE = "block_place";

    public static final String ITEM_DROP = "item_drop";
    public static final String ITEM_PICKUP = "item_pickup";

    public static final String ADVANCEMENT_DONE = "advancement_done";
    public static final String RECIPE_DISCOVER = "recipe_discover";

    public static final String PLAYER_DAMAGE = "player_damage";
    public static final String PLAYER_ATTACK = "player_attack";
    public static final String PLAYER_TOTEM = "player_totem";

    public static final String PLAYER_FISH = "player_fish";
    public static final String PLAYER_SLEEP = "player_sleep";
    public static final String PLAYER_BED_LEAVE = "player_bed_leave";

    public static final String PLAYER_CRAFT = "player_craft";
    public static final String PLAYER_SMITH = "player_smith";
    public static final String PLAYER_CONSUME = "player_consume";
    public static final String ITEM_BREAK = "item_break";

    public static final String PLAYER_BUCKET_FILL = "player_bucket_fill";
    public static final String PLAYER_BUCKET_EMPTY = "player_bucket_empty";
    public static final String PLAYER_HARVEST = "player_harvest";
    public static final String PLAYER_SHEAR = "player_shear";
    public static final String PLAYER_INTERACT = "player_interact";
    public static final String PLAYER_INTERACT_ENTITY = "player_interact_entity";
    public static final String SIGN_EDIT = "sign_edit";

    public static final String PLAYER_PORTAL = "player_portal";
    public static final String PLAYER_MOUNT = "player_mount";
    public static final String PLAYER_DISMOUNT = "player_dismount";

    public static final String PLAYER_TAME = "player_tame";
    public static final String PLAYER_TRADE = "player_trade";
    public static final String PLAYER_LEVEL_UP = "player_level_up";

    private AnalyticsEvents() {
    }
}
