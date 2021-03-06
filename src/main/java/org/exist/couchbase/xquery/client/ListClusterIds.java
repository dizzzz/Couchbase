/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2015 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.couchbase.xquery.client;

import org.exist.couchbase.shared.Constants;
import org.exist.couchbase.shared.CouchbaseClusterManager;
import org.exist.couchbase.shared.GenericExceptionHandler;
import org.exist.couchbase.xquery.CouchbaseModule;
import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import java.util.Set;

/**
 * List all Cluster Ids
 *
 * @author Dannes Wessels
 */

public class ListClusterIds extends BasicFunction {

    public final static FunctionSignature signatures[] = {
            new FunctionSignature(
                    new QName("list-cluster-ids", CouchbaseModule.NAMESPACE_URI, CouchbaseModule.PREFIX),
                    "Get all Couchbase clusterIds.",
                    new SequenceType[]{
                            //new FunctionParameterSequenceType("connection", Type.STRING, Cardinality.ONE, "Server connection string")
                    },
                    new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_MORE, "Sequence of cluster connection identifiers.")
            ),
    };

    public ListClusterIds(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {

        // User must either be DBA or in the correct group
        if (!context.getSubject().hasDbaRole() && !context.getSubject().hasGroup(Constants.COUCHBASE_GROUP)) {
            final String txt = String.format("Permission denied, user '%s' must be a DBA or be in group '%s'",
                    context.getSubject().getName(), Constants.COUCHBASE_GROUP);
            LOG.error(txt);
            throw new XPathException(this, CouchbaseModule.COBA0003, txt);
        }

        try {
            final Set<String> clientIds = CouchbaseClusterManager.getInstance().list();

            final ValueSequence valueSequence = new ValueSequence();

            clientIds.forEach((clusterId) -> valueSequence.add(new StringValue(clusterId)));

            return valueSequence;

        } catch (final Throwable ex) {
            return GenericExceptionHandler.handleException(this, ex);
        }
    }
}
