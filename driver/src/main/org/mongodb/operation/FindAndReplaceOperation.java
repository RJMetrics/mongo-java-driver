/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.operation;

import org.mongodb.CommandResult;
import org.mongodb.Decoder;
import org.mongodb.MongoNamespace;
import org.mongodb.codecs.PrimitiveCodecs;
import org.mongodb.command.Command;
import org.mongodb.command.FindAndModifyCommandResultCodec;
import org.mongodb.command.FindAndReplaceCommand;
import org.mongodb.connection.BufferProvider;
import org.mongodb.connection.ClusterDescription;
import org.mongodb.operation.protocol.CommandProtocol;
import org.mongodb.session.ServerConnectionProviderOptions;
import org.mongodb.session.Session;

import static org.mongodb.operation.CommandReadPreferenceHelper.getCommandReadPreference;
import static org.mongodb.operation.CommandReadPreferenceHelper.isQuery;

public class FindAndReplaceOperation<T> extends OperationBase<T> {
    private final FindAndReplace<T> findAndReplace;
    private final MongoNamespace namespace;
    private final ClusterDescription clusterDescription;
    private final FindAndModifyCommandResultCodec<T> findAndModifyCommandResultCodec;

    public FindAndReplaceOperation(final BufferProvider bufferProvider, final Session session, final ClusterDescription clusterDescription,
                                   final MongoNamespace namespace, final FindAndReplace<T> findAndReplace,
                                   final PrimitiveCodecs primitiveCodecs, final Decoder<T> decoder) {
        super(bufferProvider, session, false);
        this.findAndReplace = findAndReplace;
        this.namespace = namespace;
        this.clusterDescription = clusterDescription;
        findAndModifyCommandResultCodec = new FindAndModifyCommandResultCodec<T>(primitiveCodecs, decoder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T execute() {
        final FindAndReplaceCommand<T> command = new FindAndReplaceCommand<T>(findAndReplace, namespace.getCollectionName());
        final ServerConnectionProvider provider = getServerConnectionProvider(command);
        final CommandResult commandResult = new CommandProtocol(namespace.getDatabaseName(), command.toDocument(),
                                                                findAndModifyCommandResultCodec,
                                                                getBufferProvider(), provider.getServerDescription(),
                                                                provider.getConnection(), true).execute();
        return (T) commandResult.getResponse().get("value");
        // TODO: any way to remove the warning?  This could be a design flaw
    }

    //TODO: how can we reuse this effectively?
    private ServerConnectionProvider getServerConnectionProvider(final Command command) {
        final ReadPreferenceServerSelector serverSelector = new ReadPreferenceServerSelector(getCommandReadPreference(command,
                                                                                                                      clusterDescription));
        return getSession().createServerConnectionProvider(new ServerConnectionProviderOptions(isQuery(command), serverSelector));
    }
}