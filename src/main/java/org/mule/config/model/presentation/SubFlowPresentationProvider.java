package org.mule.config.model.presentation;

import com.intellij.ide.presentation.PresentationProvider;
import org.jetbrains.annotations.Nullable;
import org.mule.config.model.SubFlow;
import org.mule.util.MuleIcons;

import javax.swing.*;


public class SubFlowPresentationProvider extends PresentationProvider<SubFlow> {

    @Nullable
    @Override
    public String getName(SubFlow flow) {
        return flow.getName().getValue() != null ? "SubFlow : " + flow.getName().getValue() : "SubFlow";
    }

    @Nullable
    @Override
    public String getTypeName(SubFlow subFlow) {
        return "SubFlow";
    }


    @Nullable
    @Override
    public Icon getIcon(SubFlow flow) {
        return MuleIcons.MuleSubFlow;
    }
}
