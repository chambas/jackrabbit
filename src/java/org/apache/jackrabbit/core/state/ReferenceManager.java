/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.NodeId;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>ReferenceManager</code> ...
 */
public class ReferenceManager {

    private static Logger log = Logger.getLogger(ReferenceManager.class);

    private final PersistenceManager persistMgr;

    /**
     * A cache for <code>NodeReferences</code> objects created by this
     * <code>ReferenceManager</code>
     */
    private Map refsCache;

    /**
     * Package private constructor
     */
    public ReferenceManager(PersistenceManager persistMgr) {
        this.persistMgr = persistMgr;
        // setup cache with soft references to <code>NodeReferences</code> objects
        refsCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    /**
     * @param targetId
     * @return
     * @throws RepositoryException
     */
    public synchronized NodeReferences get(NodeId targetId) throws RepositoryException {
        if (refsCache.containsKey(targetId)) {
            return (NodeReferences) refsCache.get(targetId);
        }
        NodeReferences refs;
        try {
            // load persisted references
            refs = new NodeReferences(new NodeId(targetId.getUUID()));
            persistMgr.load(refs);
        } catch (NoSuchItemStateException nsise) {
            // does not exist, create new
            refs = new NodeReferences(new NodeId(targetId.getUUID()));
        } catch (ItemStateException ise) {
            String msg = "error while loading references";
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
        // put it in cache
        refsCache.put(targetId, refs);
        return refs;
    }

    /**
     * @param refs
     * @throws RepositoryException
     */
    public synchronized void save(NodeReferences refs) throws RepositoryException {
        if (!refs.hasReferences()) {
            remove(refs);
            return;
        }
        if (!refsCache.containsKey(refs.getTargetId())) {
            // not yet in cache, put it in cache
            refsCache.put(refs.getTargetId(), refs);
        }
        try {
            // store references
            persistMgr.store(refs);
        } catch (ItemStateException ise) {
            String msg = "error while storing references";
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @param refs
     * @throws RepositoryException
     */
    public synchronized void remove(NodeReferences refs) throws RepositoryException {
        try {
            // destroy persisted references
            persistMgr.destroy(refs);
        } catch (ItemStateException ise) {
            String msg = "error while destroying references";
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }

        // remove from cache
        refsCache.remove(refs.getTargetId());
    }
}
