/*
 * Copyright 2017 jiajunhui<junhui_jia@163.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kk.taurus.playerbase.extension;

import android.os.Bundle;

import com.kk.taurus.playerbase.receiver.IReceiverGroup;

public interface ReceiverEventSender {

    void sendEvent(int eventCode, Bundle bundle);
    void sendEvent(int eventCode, Bundle bundle, IReceiverGroup.OnReceiverFilter receiverFilter);

    void sendBoolean(String key, boolean value);
    void sendInt(String key, int value);
    void sendString(String key, String value);
    void sendFloat(String key, float value);
    void sendLong(String key, long value);
    void sendDouble(String key, double value);
    void sendObject(String key, Object value);

}
