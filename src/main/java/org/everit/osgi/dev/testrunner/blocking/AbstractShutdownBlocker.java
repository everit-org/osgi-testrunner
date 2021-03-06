/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.dev.testrunner.blocking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.everit.osgi.dev.testrunner.engine.TestClassResult;

/**
 * Helper class to be able to implement blocker easier. It handles the blockListeners in a standard
 * way. The subclass of this class should call {@link #block()} and {@link #unblock()}.
 */
public abstract class AbstractShutdownBlocker implements ShutdownBlocker {

  private boolean blocking = false;

  private final List<ShutdownBlockListener> blockListeners = new ArrayList<>();

  private final ReentrantReadWriteLock blockListenersRWLock = new ReentrantReadWriteLock(false);

  @Override
  public void addBlockListener(final ShutdownBlockListener blockListener) {
    WriteLock writeLock = blockListenersRWLock.writeLock();
    writeLock.lock();
    blockListeners.add(blockListener);
    if (blocking) {
      blockListener.block();
    }
    writeLock.unlock();
  }

  /**
   * Should be called by the subclass to notify all listeners that this blocker is blocking. If the
   * blocker is in blocking state already, nothing will happen.
   */
  protected void block() {
    notifyListeners(true);
  }

  @Override
  public void handleTestClassResult(final TestClassResult testClassResult) {
    // Do nothing
  }

  /**
   * Notifies all block listeners about either blocking or not blocking.
   *
   * @param block
   *          Whether the event is blocking or not.
   */
  private void notifyListeners(final boolean block) {
    ReadLock readLock = blockListenersRWLock.readLock();
    readLock.lock();
    if (blocking != block) {
      blocking = block;
      for (ShutdownBlockListener blockListener : blockListeners) {
        if (block) {
          blockListener.block();
        } else {
          blockListener.unblock();
        }
      }
    }
    readLock.unlock();
  }

  @Override
  public void removeBlockListener(final ShutdownBlockListener blockListener) {
    WriteLock writeLock = blockListenersRWLock.writeLock();
    writeLock.lock();
    blockListeners.remove(blockListener);
    writeLock.unlock();
  }

  /**
   * Should be called by the subclass to notify all listeners that this blocker is not blocking
   * anymore. If the blocker is in non-blocking state already, nothing will happen.
   */
  protected void unblock() {
    notifyListeners(false);
  }
}
