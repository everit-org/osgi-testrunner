package org.everit.osgi.dev.testrunner.blocking;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Helper class to be able to implement blocker easier. It handles the blockListeners in a standard way. The subclass of
 * this class should call {@link #notifyListenersAboutBlock()} and {@link #notifyListenersAboutUnblock().
 */
public abstract class AbstractBlocker implements Blocker {

    private boolean blocking = false;

    private List<BlockListener> blockListeners = new ArrayList<BlockListener>();

    private ReentrantReadWriteLock blockListenersRWLock = new ReentrantReadWriteLock(false);

    @Override
    public void addBlockListener(final BlockListener blockListener) {
        WriteLock writeLock = blockListenersRWLock.writeLock();
        writeLock.lock();
        blockListeners.add(blockListener);
        if (blocking) {
            blockListener.block();
        }
        writeLock.unlock();
    }

    protected void notifyListeners(final boolean block) {
        ReadLock readLock = blockListenersRWLock.readLock();
        readLock.lock();
        blocking = block;
        for (BlockListener blockListener : blockListeners) {
            if (block) {
                blockListener.block();
            } else {
                blockListener.unblock();
            }
        }
        readLock.unlock();
    }

    protected void notifyListenersAboutBlock() {
        notifyListeners(true);
    }

    protected void notifyListenersAboutUnblock() {
        notifyListeners(false);
    }

    @Override
    public void removeBlockListener(final BlockListener blockListener) {
        WriteLock writeLock = blockListenersRWLock.writeLock();
        writeLock.lock();
        blockListeners.remove(blockListener);
        writeLock.unlock();
    }
}
