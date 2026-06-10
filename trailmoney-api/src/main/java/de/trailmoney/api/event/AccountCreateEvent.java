package de.trailmoney.api.event;

import de.trailmoney.api.account.Account;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class AccountCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Account account;

    public AccountCreateEvent(Account account) {
        this.account = Objects.requireNonNull(account, "account");
    }

    public Account account() {
        return account;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
