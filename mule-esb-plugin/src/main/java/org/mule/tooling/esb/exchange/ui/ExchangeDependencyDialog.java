package org.mule.tooling.esb.exchange.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ListTableModel;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.tooling.esb.exchange.ExchangeArtifact;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExchangeDependencyDialog extends DialogWrapper {
    final static Logger logger = Logger.getInstance(ExchangeDependencyDialog.class);

    private final static String EXCHANGE_QUERY = "{ \"query\" : \"{ assets(query: { type: \\\"extension\\\" }) { runtimeVersion, groupId, assetId, version, classifier, name, description, status, type } }\" }";

    //TODO : we need to make it configurable
    private final static String ANYPOINT_EXCHANGE_URL = "https://anypoint.mulesoft.com/graph/api/v1/graphql";

    private JPanel contentPane;
    private JPanel mainPane;
    private ExchangeArtifactFilter filterComponent;

    private JBTable myInputsTable;

    private final ListTableModel<ExchangeArtifact> myTableModel = getTableModel();

    private ExchangeArtifact[] mySelectedArtifacts = new ExchangeArtifact[] {};

    public ExchangeDependencyDialog() {
        super(true);
        super.init();

        setModal(true);
        setTitle("Add Connector Dependency from Anypoint Exchange");

        myInputsTable = new JBTable();

        myInputsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();

                List<ExchangeArtifact> artifacts = new ArrayList<>();

                if (!lsm.isSelectionEmpty()) {
                    // Find out which indexes are selected.
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    for (int i = minIndex; i <= maxIndex; i++) {
                        if (lsm.isSelectedIndex(i)) {
                            int modelIndex = myInputsTable.convertRowIndexToModel(i);
                            artifacts.add(myTableModel.getItem(modelIndex));
                        }
                    }
                }

                mySelectedArtifacts = artifacts.toArray(new ExchangeArtifact[] {});
            }
        });

        updateTable();

        myInputsTable.getEmptyText().setText("No Connectors Found.");
        myInputsTable.setColumnSelectionAllowed(false);
        myInputsTable.setShowGrid(true);
        myInputsTable.setDragEnabled(false);
        myInputsTable.setShowHorizontalLines(true);
        myInputsTable.setShowVerticalLines(true);
        myInputsTable.setIntercellSpacing(new Dimension(0, 0));
        myInputsTable.setRowSelectionAllowed(true);
        myInputsTable.setExpandableItemsEnabled(true);
        myInputsTable.setStriped(true);
        myInputsTable.setRowHeight(myInputsTable.getRowHeight() + 10);
        myInputsTable.setAutoCreateRowSorter(true);
        myInputsTable.setVisible(true);

        mainPane.add(ScrollPaneFactory.createScrollPane(myInputsTable));

        filterComponent.getTextEditor().addKeyListener(new KeyAdapter() {
            public void keyTyped(final KeyEvent e) {
                String filterText = filterComponent.getTextEditor().getText();
                DefaultRowSorter rowSorter = (DefaultRowSorter)myInputsTable.getRowSorter();
                if (StringUtils.isEmpty(filterText))
                    rowSorter.setRowFilter(null);
                else
                    rowSorter.setRowFilter(RowFilter.regexFilter(filterComponent.getTextEditor().getText()));
            }
        });
    }

    protected ListTableModel<ExchangeArtifact> getTableModel() {
        return new ExchangeArtifactTableModel();
    }

    private void updateTable() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(ANYPOINT_EXCHANGE_URL);
            post.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(EXCHANGE_QUERY);
            post.setEntity(entity);

            StringBuffer jsonResponse = new StringBuffer();

            HttpResponse httpResponse = client.execute(post);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpResponse.getEntity().getContent()));
                String inputLine;

                while ((inputLine = reader.readLine()) != null) {
                    jsonResponse.append(inputLine);
                }
                reader.close();

                logger.debug(jsonResponse.toString());
            }
            ((CloseableHttpClient) client).close();

            final List<ExchangeArtifact> artifacts = new ArrayList<>();

            JSONObject responseObject = new JSONObject(jsonResponse.toString());
            JSONArray assets = responseObject.getJSONObject("data").getJSONArray("assets");

            for (int i = 0; i < assets.length(); i++) {
                JSONObject nextAsset = assets.getJSONObject(i);
                ExchangeArtifact nextArtifact = new ExchangeArtifact(nextAsset.getString("groupId"),
                        nextAsset.getString("assetId"),
                        StringUtils.isEmpty(nextAsset.getString("classifier")) ? "mule-plugin" : nextAsset.getString("classifier"),
                        nextAsset.getString("version"),
                        nextAsset.getString("runtimeVersion"),
                        nextAsset.getString("name"),
                        nextAsset.getString("description"));
                artifacts.add(nextArtifact);
            }

            myTableModel.setSortable(true);
            myTableModel.setItems(artifacts);
            myInputsTable.setModel(myTableModel);

            //if (!artifacts.isEmpty()) {
            //    myInputsTable.getSelectionModel().setSelectionInterval(0, 0);
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ExchangeArtifact[] getSelectedArtifacts() {
        return mySelectedArtifacts;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    public JPanel getContentPane() {
        return contentPane;
    }

    public void setContentPane(JPanel contentPane) {
        this.contentPane = contentPane;
    }

    public JPanel getMainPane() {
        return mainPane;
    }

    public void setMainPane(JPanel mainPane) {
        this.mainPane = mainPane;
    }

    public JBTable getMyInputsTable() {
        return myInputsTable;
    }

    public void setMyInputsTable(JBTable myInputsTable) {
        this.myInputsTable = myInputsTable;
    }
}
