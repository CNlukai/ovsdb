/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;

/**
 * The NetworkingProvider interface is implemented by Neutron Networking Providers
 */
public interface NetworkingProvider {

    /**
     * Returns the name of the NetworkingProvider
     */
    public String getName();

    /**
     * Return true if the provider supports Network Service Instances
     */
    public boolean supportsServices();

    /**
     * Return true if the provider supports per-tenant or "static" tunneling
     */
    public boolean hasPerTenantTunneling();

    /**
     * Handle Interface Update Callback Method
     */
    public boolean handleInterfaceUpdate(NeutronNetwork network, Node source, OvsdbTerminationPointAugmentation intf);

    /**
     * Handle Interface Delete Callback Method
     */
    public boolean handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node source,
                                         OvsdbTerminationPointAugmentation intf, boolean isLastInstanceOnNode);

    /**
     * Initialize the Flow rules given the OVSDB node.
     * This method provides a set of common functionalities to initialize the Flow rules of an OVSDB node
     * that are Openflow Version specific. Hence we have this method in addition to the following
     * Openflow Node specific initialization method.
     */
    public void initializeFlowRules(Node node);

    /**
     * Initialize the Flow rules for a given OpenFlow node
     */
    public void initializeOFFlowRules(Node openflowNode);
}
