/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global nf */

/**
 * Views state for a given component.
 */
nf.ComponentState = (function () {

    var config = {
        filterText: 'Filter',
        styles: {
            filterList: 'filter-list'
        }
    };

    /**
     * Filters the component state table.
     */
    var applyFilter = function () {
        // get the dataview
        var componentStateTable = $('#component-state-table').data('gridInstance');

        // ensure the grid has been initialized
        if (nf.Common.isDefinedAndNotNull(componentStateTable)) {
            var componentStateData = componentStateTable.getData();

            // update the search criteria
            componentStateData.setFilterArgs({
                searchString: getFilterText()
            });
            componentStateData.refresh();
        }
    };

    /**
     * Determines if the item matches the filter.
     *
     * @param {object} item     The item to filter
     * @param {object} args     The filter criteria
     * @returns {boolean}       Whether the item matches the filter
     */
    var filter = function (item, args) {
        if (args.searchString === '') {
            return true;
        }

        try {
            // perform the row filtering
            var filterExp = new RegExp(args.searchString, 'i');
        } catch (e) {
            // invalid regex
            return false;
        }

        // determine if the item matches the filter
        var matchesKey = item['key'].search(filterExp) >= 0;
        var matchesValue = item['value'].search(filterExp) >= 0;

        // conditionally consider the scope
        var matchesScope = false;
        if (nf.Common.isDefinedAndNotNull(item['scope'])) {
            matchesScope = item['scope'].search(filterExp) >= 0;
        }

        return matchesKey || matchesValue || matchesScope;
    };

    /**
     * Sorts the specified data using the specified sort details.
     *
     * @param {object} sortDetails
     * @param {object} data
     */
    var sort = function (sortDetails, data) {
        // defines a function for sorting
        var comparer = function (a, b) {
            var aString = nf.Common.isDefinedAndNotNull(a[sortDetails.columnId]) ? a[sortDetails.columnId] : '';
            var bString = nf.Common.isDefinedAndNotNull(b[sortDetails.columnId]) ? b[sortDetails.columnId] : '';
            return aString === bString ? 0 : aString > bString ? 1 : -1;
        };

        // perform the sort
        data.sort(comparer, sortDetails.sortAsc);
    };

    /**
     * Get the text out of the filter field. If the filter field doesn't
     * have any text it will contain the text 'filter list' so this method
     * accounts for that.
     */
    var getFilterText = function () {
        var filterText = '';
        var filterField = $('#component-state-filter');
        if (!filterField.hasClass(config.styles.filterList)) {
            filterText = filterField.val();
        }
        return filterText;
    };

    /**
     * Clears the component state table.
     */
    var clearTable = function () {
        var componentStateGrid = $('#component-state-table').data('gridInstance');
        var componentStateData = componentStateGrid.getData();
        componentStateData.setItems([]);

        // hide the partial results details message
        $('#component-state-partial-results-container').hide();

        // clear the total number entries
        $('#displayed-component-state-entries').text('0');
        $('#total-component-state-entries').text('0');
    };

    /**
     * Loads the table with the component state.
     *
     * @param {object} componentState
     */
    var loadComponentState = function (localState, clusterState, externalState) {
        var count = 0;
        var totalEntries = 0;
        var showPartialDetails = false;

        var componentStateGrid = $('#component-state-table').data('gridInstance');
        var componentStateData = componentStateGrid.getData();

        // begin the update
        componentStateData.beginUpdate();

        var addComponentStateData = function (componentState, getScope) {
            if (nf.Common.isDefinedAndNotNull(componentState)
                && nf.Common.isDefinedAndNotNull(componentState.state)) {

                $.each(componentState.state, function (i, stateEntry) {
                    componentStateData.addItem($.extend({
                        id: count++,
                        scope: getScope(stateEntry),
                    }, stateEntry));
                });
                totalEntries += componentState.totalEntryCount;

                if (componentState.totalEntryCount !== componentState.state.length) {
                    showPartialDetails = true;
                }
            }
        }

        addComponentStateData(localState, function(stateEntry) {
            return stateEntry.clusterNodeAddress;
        });

        addComponentStateData(clusterState, function(stateEntry) {
            return 'Cluster';
        });

        addComponentStateData(externalState, function(stateEntry) {
            return 'External' + (stateEntry.clusterNodeAddress ? ' - ' + stateEntry.clusterNodeAddress : '');
        });

        // complete the update
        componentStateData.endUpdate();
        componentStateData.reSort();

        if (showPartialDetails) {
            $('#component-state-partial-results-container').show();
        }

        // update the total number of state entries
        $('#total-component-state-entries').text(nf.Common.formatInteger(totalEntries));
    };

    /**
     * Reset the dialog.
     */
    var resetDialog = function () {
        // clear the fields
        $('#component-state-name').text('');
        $('#component-state-description').text('');

        // clear any filter strings
        $('#component-state-filter').addClass(config.styles.filterList).val(config.filterText);

        // reset clear link
        $('#clear-link').removeClass('disabled').attr('title', '');

        // clear the table
        clearTable();

        // clear the component
        $('#component-state-table').removeData('component');
    };

    return {
        init: function () {
            // intialize the component state filter
            $('#component-state-filter').on('focus', function () {
                if ($(this).hasClass(config.styles.filterList)) {
                    $(this).removeClass(config.styles.filterList).val('');
                }
            }).on('blur', function () {
                if ($(this).val() === '') {
                    $(this).addClass(config.styles.filterList).val(config.filterText);
                }
            }).on('keyup', function () {
                applyFilter();
            }).addClass(config.styles.filterList).val(config.filterText);

            // initialize the processor configuration dialog
            $('#component-state-dialog').modal({
                scrollableContentStyle: 'scrollable',
                headerText: 'Component State',
                buttons: [{
                    buttonText: 'Close',
                    color: {
                        base: '#728E9B',
                        hover: '#004849',
                        text: '#ffffff'
                    },
                    handler: {
                        click: function () {
                            $(this).modal('hide');
                        }
                    }
                }],
                handler: {
                    close: function () {
                        resetDialog();
                    }
                }
            });

            // clear state link
            $('#clear-link').on('click', function () {
                if ($(this).hasClass('disabled') === false) {
                    var componentStateTable = $('#component-state-table');

                    // ensure there is state to clear
                    var componentStateGrid = componentStateTable.data('gridInstance');
                    var stateEntryCount = componentStateGrid.getDataLength();

                    if (stateEntryCount > 0) {
                        var componentEntity = componentStateTable.data('component');
                        $.ajax({
                            type: 'POST',
                            url: componentEntity.uri + '/state/clear-requests',
                            dataType: 'json'
                        }).done(function (response) {
                            if (response.cleared) {
                                // clear the table
                                clearTable();

                                // reload the table with no state
                                loadComponentState()
                            } else {
                                nf.Dialog.showOkDialog({
                                    headerText: 'Failed to clear state',
                                    dialogContent: response.message
                                });
                            }

                        }).fail(nf.Common.handleAjaxError);
                    } else {
                        nf.Dialog.showOkDialog({
                            headerText: 'Component State',
                            dialogContent: 'This component has no state to clear.'
                        });
                    }
                }
            });

            // initialize the queue listing table
            var componentStateColumns = [
                {id: 'key', field: 'key', name: 'Key', sortable: true, resizable: true},
                {id: 'value', field: 'value', name: 'Value', sortable: true, resizable: true}
            ];

            var componentStateOptions = {
                forceFitColumns: true,
                enableTextSelectionOnCells: true,
                enableCellNavigation: false,
                enableColumnReorder: false,
                autoEdit: false,
                rowHeight: 24
            };

            // initialize the dataview
            var componentStateData = new Slick.Data.DataView({
                inlineFilters: false
            });
            componentStateData.setItems([]);
            componentStateData.setFilterArgs({
                searchString: '',
                property: 'key'
            });
            componentStateData.setFilter(filter);

            // initialize the sort
            sort({
                columnId: 'key',
                sortAsc: true
            }, componentStateData);

            // initialize the grid
            var componentStateGrid = new Slick.Grid('#component-state-table', componentStateData, componentStateColumns, componentStateOptions);
            componentStateGrid.setSelectionModel(new Slick.RowSelectionModel());
            componentStateGrid.registerPlugin(new Slick.AutoTooltips());
            componentStateGrid.setSortColumn('key', true);
            componentStateGrid.onSort.subscribe(function (e, args) {
                sort({
                    columnId: args.sortCol.field,
                    sortAsc: args.sortAsc
                }, componentStateData);
            });

            // wire up the dataview to the grid
            componentStateData.onRowCountChanged.subscribe(function (e, args) {
                componentStateGrid.updateRowCount();
                componentStateGrid.render();

                // update the total number of displayed items
                $('#displayed-component-state-entries').text(args.current);
            });
            componentStateData.onRowsChanged.subscribe(function (e, args) {
                componentStateGrid.invalidateRows(args.rows);
                componentStateGrid.render();
            });

            // hold onto an instance of the grid
            $('#component-state-table').data('gridInstance', componentStateGrid);

            // initialize the number of display items
            $('#displayed-component-state-entries').text('0');
            $('#total-component-state-entries').text('0');
        },

        /**
         * Shows the state for a given component.
         *
         * @param {object} componentEntity
         * @param {boolean} canClear
         */
        showState: function (componentEntity, canClear) {
            return $.ajax({
                type: 'GET',
                url: componentEntity.uri + '/state',
                dataType: 'json'
            }).done(function (response) {
                var componentState = response.componentState;
                var componentStateTable = $('#component-state-table');

                // conditionally show the cluster node identifier
                var gridInstance = componentStateTable.data().gridInstance;
                var componentStateColumns = gridInstance.getColumns();
                if (nf.Canvas.isClustered() || nf.Common.isDefinedAndNotNull(componentState.externalState)) {
                    if (componentStateColumns.length === 2) {
                        // Show scope column
                        componentStateColumns.push({id: 'scope', field: 'scope', name: 'Scope', sortable: true, resizable: true});
                        gridInstance.setColumns(componentStateColumns);
                    }
                } else {
                    if (componentStateColumns.length === 3) {
                        // Hide scope columns
                        componentStateColumns.pop();
                        gridInstance.setColumns(componentStateColumns);
                    }
                }

                // load the table
                loadComponentState(componentState.localState, componentState.clusterState, componentState.externalState);

                // populate the name/description
                $('#component-state-name').text(componentEntity.component.name);
                $('#component-state-description').text(componentState.stateDescription).ellipsis();

                // store the component
                componentStateTable.data('component', componentEntity);

                // show the dialog
                $('#component-state-dialog').modal('show');

                // only activate the link when appropriate
                if (canClear === false) {
                    $('#clear-link').addClass('disabled').attr('title', 'Component state can only be cleared when the component is not actively running');
                }

                // reset the grid size
                var componentStateGrid = componentStateTable.data('gridInstance');
                componentStateGrid.resizeCanvas();
            }).fail(nf.Common.handleAjaxError);
        }
    };
}());