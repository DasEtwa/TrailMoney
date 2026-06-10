package de.trailmoney.api.event;

import de.trailmoney.api.transaction.Transaction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class TransactionPreEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Transaction transaction;
    private boolean cancelled;
    private String cancellationReason;

    public TransactionPreEvent(Transaction transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    public Transaction transaction() {
        return transaction;
    }

    public String cancellationReason() {
        return cancellationReason;
    }

    public void cancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
