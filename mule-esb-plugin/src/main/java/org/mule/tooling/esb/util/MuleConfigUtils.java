package org.mule.tooling.esb.util;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NonNavigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.ComplexTypeDescriptor;
import com.intellij.xml.impl.schema.TypeDescriptor;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import com.mulesoft.mule.debugger.commons.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mule.tooling.esb.config.MuleConfigConstants;
import org.mule.tooling.esb.config.model.Flow;
import org.mule.tooling.esb.config.model.Mule;
import org.mule.tooling.esb.config.model.SubFlow;
import org.mule.tooling.esb.framework.facet.MuleFacet;
import org.mule.tooling.esb.framework.facet.MuleFacetConfiguration;
import org.mule.tooling.esb.framework.facet.MuleFacetType;
import org.mule.tooling.esb.sdk.MuleSdk;
import org.mule.tooling.lang.dw.parser.psi.WeavePsiUtils;

import javax.xml.namespace.QName;
import java.util.*;

public class MuleConfigUtils {
    public static final String CONFIG_RELATIVE_PATH = "src/main/app";

    public static final String MULE_LOCAL_NAME = "mule";
    public static final String MULE_FLOW_LOCAL_NAME = "flow";
    public static final String MULE_SUB_FLOW_LOCAL_NAME = "sub-flow";
    public static final String MUNIT_TEST_LOCAL_NAME = "test";
    public static final String MUNIT_NAMESPACE = "munit";

    public static final String EXCEPTION_STRATEGY_LOCAL_NAME = "exception-strategy";
    public static final String CHOICE_EXCEPTION_STRATEGY_LOCAL_NAME = "choice-exception-strategy";
    public static final String ROLLBACK_EXCEPTION_STRATEGY_LOCAL_NAME = "rollback-exception-strategy";
    public static final String CATCH_EXCEPTION_STRATEGY_LOCAL_NAME = "catch-exception-strategy";

    public static boolean isMuleFile(PsiFile psiFile) {
        if (!(psiFile instanceof XmlFile)) {
            return false;
        }
        if (psiFile.getFileType() != StdFileTypes.XML) {
            return false;
        }
        final XmlFile psiFile1 = (XmlFile) psiFile;
        final XmlTag rootTag = psiFile1.getRootTag();
        return isMuleTag(rootTag);
    }

    public static boolean isMUnitFile(PsiFile psiFile) {
        if (!(psiFile instanceof XmlFile)) {
            return false;
        }
        if (psiFile.getFileType() != StdFileTypes.XML) {
            return false;
        }
        final XmlFile psiFile1 = (XmlFile) psiFile;
        final XmlTag rootTag = psiFile1.getRootTag();
        if (rootTag == null || !isMuleTag(rootTag)) {
            return false;
        }
        final XmlTag[] munitTags = rootTag.findSubTags(MUNIT_TEST_LOCAL_NAME, rootTag.getNamespaceByPrefix(MUNIT_NAMESPACE));
        return munitTags.length > 0;
    }

    public static boolean isMuleTag(XmlTag rootTag) {
        return rootTag.getLocalName().equalsIgnoreCase(MULE_LOCAL_NAME);
    }

    public static boolean isFlowTag(XmlTag rootTag) {
        return rootTag.getLocalName().equalsIgnoreCase(MULE_FLOW_LOCAL_NAME);
    }

    public static boolean isExceptionStrategyTag(XmlTag rootTag) {
        return rootTag.getLocalName().equalsIgnoreCase(EXCEPTION_STRATEGY_LOCAL_NAME) ||
                rootTag.getLocalName().equalsIgnoreCase(CHOICE_EXCEPTION_STRATEGY_LOCAL_NAME) ||
                rootTag.getLocalName().equalsIgnoreCase(CATCH_EXCEPTION_STRATEGY_LOCAL_NAME) ||
                rootTag.getLocalName().equalsIgnoreCase(ROLLBACK_EXCEPTION_STRATEGY_LOCAL_NAME);
    }

    public static boolean isSubFlowTag(XmlTag rootTag) {
        return rootTag.getLocalName().equalsIgnoreCase(MULE_SUB_FLOW_LOCAL_NAME);
    }

    public static boolean isMUnitTestTag(XmlTag rootTag) {
        return rootTag.getLocalName().equalsIgnoreCase(MUNIT_TEST_LOCAL_NAME);
    }

    public static boolean isTopLevelTag(XmlTag tag) {
        return isFlowTag(tag) || isSubFlowTag(tag) || isMUnitTestTag(tag) || isExceptionStrategyTag(tag);
    }

    public static boolean isInTopLevelTag(XmlTag tag) {
        boolean inTopLevel = false;
        XmlTag current = tag;

        while (!inTopLevel && current != null) {
            inTopLevel = MuleConfigUtils.isTopLevelTag(current);
            if (!inTopLevel)
                current = current.getParentTag();
        }

        return inTopLevel;
    }

    public static QName getQName(XmlTag xmlTag) {
        return new QName(xmlTag.getNamespace(), xmlTag.getLocalName());
    }

    @Nullable
    public static XmlTag findFlow(Project project, String flowName) {
        final GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        return findFlowInScope(project, flowName, searchScope);
    }


    @Nullable
    public static XmlTag findGlobalElement(PsiElement element, String elementName) {
        final Project project = element.getProject();
        final PsiFile psiFile = PsiTreeUtil.getParentOfType(element, PsiFile.class);
        //Search first in the local file else we search globally
        if (psiFile != null) {
            final XmlTag xmlTag = findGlobalElementInFile(project, elementName, psiFile.getVirtualFile());
            if (xmlTag != null) {
                return xmlTag;
            }
        }
        final GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        return findGlobalElementInScope(project, elementName, searchScope);
    }

    @Nullable
    public static XmlTag findFlow(PsiElement element, String flowName) {
        final Project project = element.getProject();
        final PsiFile psiFile = PsiTreeUtil.getParentOfType(element, PsiFile.class);
        //Search first in the local file else we search globally
        if (psiFile != null) {
            final XmlTag xmlTag = findFlowInFile(project, flowName, psiFile.getVirtualFile());
            if (xmlTag != null) {
                return xmlTag;
            }
        }
        final GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        return findFlowInScope(project, flowName, searchScope);
    }


    @Nullable
    public static XmlTag findFlow(Module module, String flowName) {
        final GlobalSearchScope searchScope = GlobalSearchScope.moduleScope(module);
        return findFlowInScope(module.getProject(), flowName, searchScope);
    }

    public static List<DomElement> getFlows(Module module) {
        final GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesScope(module);
        return getFlowsInScope(module.getProject(), searchScope);
    }

    public static List<DomElement> getFlows(Project project) {
        final GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        return getFlowsInScope(project, searchScope);
    }

    @NotNull
    private static List<DomElement> getFlowsInScope(Project project, GlobalSearchScope searchScope) {
        final List<DomElement> result = new ArrayList<>();
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(StdFileTypes.XML, searchScope);
        final DomManager manager = DomManager.getDomManager(project);
        for (VirtualFile file : files) {
            final PsiFile xmlFile = PsiManager.getInstance(project).findFile(file);
            if (isMuleFile(xmlFile)) {
                final DomFileElement<Mule> fileElement = manager.getFileElement((XmlFile) xmlFile, Mule.class);
                if (fileElement != null) {
                    final Mule rootElement = fileElement.getRootElement();
                    result.addAll(rootElement.getFlows());
                    result.addAll(rootElement.getSubFlows());
                }
            }
        }
        return result;
    }

    @Nullable
    private static XmlTag findGlobalElementInScope(Project project, String elementName, GlobalSearchScope searchScope) {
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(StdFileTypes.XML, searchScope);
        for (VirtualFile file : files) {
            XmlTag flow = findGlobalElementInFile(project, elementName, file);
            if (flow != null) {
                return flow;
            }
        }
        return null;
    }

    @Nullable
    private static XmlTag findFlowInScope(Project project, String flowName, GlobalSearchScope searchScope) {
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(StdFileTypes.XML, searchScope);
        for (VirtualFile file : files) {
            XmlTag flow = findFlowInFile(project, flowName, file);
            if (flow != null) {
                return flow;
            }
        }
        return null;
    }


    @Nullable
    private static XmlTag findGlobalElementInFile(Project project, String elementName, VirtualFile file) {
        final DomManager manager = DomManager.getDomManager(project);
        final PsiFile xmlFile = PsiManager.getInstance(project).findFile(file);
        if (isMuleFile(xmlFile)) {
            final DomFileElement<Mule> fileElement = manager.getFileElement((XmlFile) xmlFile, Mule.class);
            if (fileElement != null) {
                final Mule rootElement = fileElement.getRootElement();
                final XmlTag[] subTags = rootElement.getXmlTag().getSubTags();
                for (XmlTag subTag : subTags) {
                    if (isGlobalElement(subTag)) {
                        if (elementName.equals(subTag.getAttributeValue(MuleConfigConstants.NAME_ATTRIBUTE))) {
                            return subTag;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static XmlTag findFlowInFile(Project project, String flowName, VirtualFile file) {
        final DomManager manager = DomManager.getDomManager(project);
        final PsiFile xmlFile = PsiManager.getInstance(project).findFile(file);
        if (isMuleFile(xmlFile)) {
            final DomFileElement<Mule> fileElement = manager.getFileElement((XmlFile) xmlFile, Mule.class);
            if (fileElement != null) {
                final Mule rootElement = fileElement.getRootElement();
                final List<Flow> flows = rootElement.getFlows();
                for (Flow flow : flows) {
                    if (flowName.equals(flow.getName().getValue())) {
                        return flow.getXmlTag();
                    }
                }
                final List<SubFlow> subFlows = rootElement.getSubFlows();
                for (SubFlow subFlow : subFlows) {
                    if (flowName.equals(subFlow.getName().getValue())) {
                        return subFlow.getXmlTag();
                    }
                }
            }
        }
        return null;
    }


    public static MessageProcessorPath fromPath(String path) {
        final ArrayList<MessageProcessorPathNode> elements = new ArrayList<>();
        final List<String> tokens = new MessageProcessorPathTokenizer().tokens(path);
        String flowName = null;
        MessageProcessorPathType type = MessageProcessorPathType.unknown;
        for (String token : tokens) {
            if (flowName == null) {
                flowName = MulePathUtils.unescape(token);
            } else if (type == MessageProcessorPathType.unknown) {
                try {
                    type = MessageProcessorPathType.valueOf(token);
                } catch (IllegalArgumentException iae) {
                    //Ignore
                }
            } else if (isElementNumber(token)) {
                elements.add(new MessageProcessorPathNode(flowName, token));
            } else {
                flowName = MulePathUtils.unescape(token);
                elements.clear();
                type = MessageProcessorPathType.unknown;
            }
        }

        return new MessageProcessorPath(flowName, type, elements);
    }

    private static boolean isElementNumber(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    @Nullable
    public static XmlTag getTagAt(Project project, String path) {
        final MessageProcessorPath messageProcessorPath = fromPath(path);
        final MessageProcessorPathType type = messageProcessorPath.getType();
        final String flowName = messageProcessorPath.getFlowName();
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(StdFileTypes.XML, GlobalSearchScope.projectScope(project));
        final DomManager manager = DomManager.getDomManager(project);
        for (VirtualFile file : files) {
            final PsiFile xmlFile = PsiManager.getInstance(project).findFile(file);
            if (isMuleFile(xmlFile)) {
                final DomFileElement<Mule> fileElement = manager.getFileElement((XmlFile) xmlFile, Mule.class);
                if (fileElement != null) {
                    final Mule rootElement = fileElement.getRootElement();
                    switch (type) {
                        case processors:
                            final List<Flow> flows = rootElement.getFlows();
                            for (Flow flow : flows) {
                                final XmlAttributeValue xmlAttributeValue = flow.getName().getXmlAttributeValue();
                                if (xmlAttributeValue != null) {
                                    if (flowName.equals(xmlAttributeValue.getValue())) {
                                        XmlTag xmlTag = flow.getXmlTag();
                                        return findChildMessageProcessorByPath(messageProcessorPath, xmlTag);
                                    }
                                }
                            }
                            break;
                        case subprocessors:
                            final List<SubFlow> subFlows = rootElement.getSubFlows();
                            for (SubFlow subFlow : subFlows) {
                                final XmlAttributeValue xmlAttributeValue = subFlow.getName().getXmlAttributeValue();
                                if (xmlAttributeValue != null && xmlAttributeValue.getValue().equals(flowName)) {
                                    XmlTag xmlTag = subFlow.getXmlTag();
                                    return findChildMessageProcessorByPath(messageProcessorPath, xmlTag);
                                }
                            }
                            break;
                        default:
                            final XmlTag rootTag = ((XmlFile) xmlFile).getRootTag();
                            if (rootTag != null) {
                                final XmlTag[] subTags = rootTag.getSubTags();
                                for (XmlTag subTag : subTags) {
                                    final XmlAttribute name = subTag.getAttribute(MuleConfigConstants.NAME_ATTRIBUTE);
                                    if (name != null && name.getValue() != null && name.getValue().equals(flowName)) {
                                        return findChildMessageProcessorByPath(messageProcessorPath, subTag);
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }
        return null;
    }

    private static XmlTag findChildMessageProcessorByPath(MessageProcessorPath messageProcessorPath, XmlTag xmlTag) {
        final List<MessageProcessorPathNode> nodes = messageProcessorPath.getNodes();
        for (MessageProcessorPathNode node : nodes) {
            final String elementName = node.getElementName();
            final int i = Integer.parseInt(elementName);
            final XmlTag[] subTags = xmlTag.getSubTags();
            int index = -1;
            for (XmlTag subTag : subTags) {
                final MuleElementType muleElementType = getMuleElementTypeFromXmlElement(subTag);
                if (muleElementType == MuleElementType.MESSAGE_PROCESSOR) {
                    xmlTag = subTag;
                    index = index + 1;
                }
                if (index == i) {
                    break;
                }
            }
        }
        return xmlTag;
    }


    @Nullable
    public static XSourcePosition createPositionByElement(PsiElement element) {
        if (element == null)
            return null;

        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null)
            return null;

        final VirtualFile file = psiFile.getVirtualFile();
        if (file == null)
            return null;

        final SmartPsiElementPointer<PsiElement> pointer =
                SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

        return new XSourcePosition() {
            private volatile XSourcePosition myDelegate;

            private XSourcePosition getDelegate() {
                if (myDelegate == null) {
                    myDelegate = ApplicationManager.getApplication().runReadAction(new Computable<XSourcePosition>() {
                        @Override
                        public XSourcePosition compute() {
                            PsiElement elem = pointer.getElement();
                            return XSourcePositionImpl.createByOffset(pointer.getVirtualFile(), elem != null ? elem.getTextOffset() : -1);
                        }
                    });
                }
                return myDelegate;
            }

            @Override
            public int getLine() {
                return getDelegate().getLine();
            }

            @Override
            public int getOffset() {
                return getDelegate().getOffset();
            }

            @NotNull
            @Override
            public VirtualFile getFile() {
                return file;
            }

            @NotNull
            @Override
            public Navigatable createNavigatable(@NotNull Project project) {
                // no need to create delegate here, it may be expensive
                if (myDelegate != null) {
                    return myDelegate.createNavigatable(project);
                }
                PsiElement elem = pointer.getElement();
                if (elem instanceof Navigatable) {
                    return ((Navigatable) elem);
                }
                return NonNavigatable.INSTANCE;
            }
        };
    }

    @NotNull
    public static Breakpoint toMuleBreakpoint(Project project, XLineBreakpoint<XBreakpointProperties> lineBreakpoint, @Nullable Map<String, String> modulesToAppsMap) {
        final XSourcePosition sourcePosition = lineBreakpoint.getSourcePosition();
        final XExpression conditionExpression = lineBreakpoint.getConditionExpression();
        return toMuleBreakpoint(project, sourcePosition, conditionExpression, modulesToAppsMap);
    }

    @NotNull
    public static Breakpoint toMuleBreakpoint(Project project, XLineBreakpoint<XBreakpointProperties> lineBreakpoint) {
        return toMuleBreakpoint(project, lineBreakpoint, null);
    }

    @NotNull
    public static Breakpoint toMuleBreakpoint(Project project, @NotNull XSourcePosition sourcePosition, XExpression conditionExpression, @Nullable Map<String, String> modulesToAppsMap) {
        VirtualFile file = sourcePosition.getFile();
        Module module = ModuleUtilCore.findModuleForFile(file, project);

        String deployableName = module.getName();

        if (modulesToAppsMap != null && !StringUtils.isEmpty(modulesToAppsMap.get(deployableName)))
            deployableName = modulesToAppsMap.get(deployableName);

        boolean isMule4 = MuleConfigUtils.isMule4Module(module);
        //If Mule 4 - add "mule-application"
        if (isMule4)
            deployableName = deployableName + "-mule-application";

        final String conditionScript = conditionExpression != null ? asMelScript(conditionExpression.getExpression()) : null;
        final XmlTag tag = getXmlTagAt(module.getProject(), sourcePosition);
        if (tag != null) {
            //TODO - Module name is an app name - but can I get it from Maven? Or override it by using the additional param?
            return new Breakpoint(getMulePath(tag, isMule4), conditionScript, deployableName);
        } else {
            final int line = sourcePosition.getLine();
            final Document document = FileDocumentManager.getInstance().getDocument(sourcePosition.getFile());
            final PsiElement xmlElement = WeavePsiUtils.getFirstWeaveElement(module.getProject(), document, line);
            if (xmlElement != null) {
                PsiLanguageInjectionHost parent = PsiTreeUtil.getParentOfType(xmlElement, PsiLanguageInjectionHost.class);
                if (parent != null) {
                    final XmlTag weavePart = PsiTreeUtil.getParentOfType(xmlElement, XmlTag.class);
                    final XmlTag weaveTag = PsiTreeUtil.getParentOfType(weavePart, XmlTag.class);
                    int lineNumber = line + 1 - XSourcePositionImpl.createByElement(xmlElement).getLine();
                    final String mulePath = getMulePath(weaveTag, isMule4);
                    //TODO - Module name is an app name - but can I get it from Maven? Or override it by using the additional param?
                    return new Breakpoint(mulePath, getPrefix(weavePart) + "/" + (lineNumber + 1), conditionScript, deployableName);
                }
            }
        }
        return new Breakpoint("", conditionScript, deployableName);
    }

    @NotNull
    private static String getPrefix(XmlTag weavePart) {
        final String localName = weavePart.getLocalName();
        if (localName.equals("set-payload")) {
            return "payload:";
        } else if (localName.equals("set-variable")) {
            return "flowVar:" + weavePart.getAttributeValue("variableName");
        } else if (localName.equals("set-property")) {
            return "property:" + weavePart.getAttributeValue("propertyName");
        } else if (localName.equals("set-session-variable")) {
            return "sessionVar:" + weavePart.getAttributeValue("variableName");
        }

        return "payload:";
    }


    @Nullable
    public static XmlTag getXmlTagAt(Project project, XSourcePosition sourcePosition) {
        final VirtualFile file = sourcePosition.getFile();
        final XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(file);
        final XmlTag rootTag = xmlFile.getRootTag();
        return findXmlTag(sourcePosition, rootTag);
    }

    private static XmlTag findXmlTag(XSourcePosition sourcePosition, XmlTag rootTag) {
        final XmlTag[] subTags = rootTag.getSubTags();
        for (int i = 0; i < subTags.length; i++) {
            XmlTag subTag = subTags[i];
            final int subTagLineNumber = getLineNumber(sourcePosition.getFile(), subTag);
            if (subTagLineNumber == sourcePosition.getLine()) {
                return subTag;
            } else if (subTagLineNumber > sourcePosition.getLine() && i > 0 && subTags[i - 1].getSubTags().length > 0) {
                return findXmlTag(sourcePosition, subTags[i - 1]);
            }
        }
        if (subTags.length > 0) {
            final XmlTag lastElement = subTags[subTags.length - 1];
            return findXmlTag(sourcePosition, lastElement);
        } else {
            return null;
        }
    }

    public static int getLineNumber(VirtualFile file, XmlTag tag) {
        final int offset = tag.getTextOffset();
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        return offset < document.getTextLength() ? document.getLineNumber(offset) : -1;
    }

    public static String getMulePath(XmlTag tag) {
        return getMulePath(tag, false);
    }

    public static String getMulePath(XmlTag tag, boolean isMule4) {
        final LinkedList<XmlTag> elements = new LinkedList<>();
        while (!isMuleTag(tag)) {
            elements.push(tag);
            tag = tag.getParentTag();
        }
        String path = "";
        for (int i = 0; i < elements.size(); i++) {
            final XmlTag element = elements.get(i);
            switch (i) {
                case 0: {
                    final XmlAttribute name = element.getAttribute(MuleConfigConstants.NAME_ATTRIBUTE);
                    if (name != null) {
                        //path = "/" + MulePathUtils.escape(name.getValue()) + getGlobalElementCategory(element);
                        path = MulePathUtils.escape(name.getValue()) + getGlobalElementCategory(element);
                        if (!isMule4)
                            path = "/" + path;
                    }
                    break;
                }
                default: {
                    final XmlTag parentTag = element.getParentTag();
                    int index = 0;
                    for (XmlTag xmlTag : parentTag.getSubTags()) {
                        if (xmlTag == element) {
                            break;
                        }
                        final MuleElementType muleElementType = getMuleElementTypeFromXmlElement(xmlTag);
                        if (muleElementType == MuleElementType.MESSAGE_PROCESSOR) {
                            index = index + 1;
                        }
                    }
                    path = path + "/" + index;
                }
            }
        }
        System.out.println("path = " + path);
        return path;
    }

    @Nullable
    public static MuleElementType getMuleElementTypeFromXmlElement(XmlTag xmlTag) {
        final XmlElementDescriptor descriptor = xmlTag.getDescriptor();
        if (descriptor instanceof XmlElementDescriptorImpl) {
            final XmlElementDescriptorImpl xmlElementDescriptor = (XmlElementDescriptorImpl) descriptor;
            final TypeDescriptor schemaType = xmlElementDescriptor.getType();
            if (schemaType instanceof ComplexTypeDescriptor) {
                final XmlTag complexTypeTag = schemaType.getDeclaration();
                final MuleElementType typeReference = MuleSchemaUtils.getElementTypeFromComplexType(complexTypeTag);
                if (typeReference != null) {
                    return typeReference;
                }
            }
        } else {
            //This is to handle unknown elements
            if (!(xmlTag.getName().equals("description")) && isContainer(xmlTag.getParentTag())) {
                return (isKnownMS(xmlTag)) ? MuleElementType.MESSAGE_SOURCE : MuleElementType.MESSAGE_PROCESSOR;
            }
        }
        return null;
    }

    private static boolean isKnownMS(XmlTag xmlTag) {
        return xmlTag.getName().equals("poll") || xmlTag.getName().endsWith("listener") || xmlTag.getName().equals("inbound-endpoint");
    }

    private static boolean isContainer(XmlTag parentTag) {
        return parentTag.getName().equals("flow")
                || parentTag.getName().equals("sub-flow")
                || parentTag.getName().equals("foreach")
                || parentTag.getName().equals("when")
                || parentTag.getName().equals("otherwise")
                || parentTag.getName().equals("cache")
                || parentTag.getName().equals("enricher")
                || parentTag.getName().equals("processor-chain")
                || parentTag.getName().equals("scatter-gather")
                || parentTag.getName().equals("all")
                ;
    }


    @NotNull
    private static String getGlobalElementCategory(XmlTag element) {
        switch (element.getLocalName()) {
            case "flow":
                return "/processors";
            case "sub-flow":
                return "/subprocessors";
            case "test":
                return "/tests";
            default:
                return "/es";
        }

    }

    @NotNull
    public static String asMelScript(@NotNull String script) {
        return !script.startsWith("#[") ? "#[" + script + "]" : script;
    }

    public static List<XmlTag> getGlobalElements(Project project) {
        return getGlobalElementsInScope(project, GlobalSearchScope.allScope(project));
    }

    @NotNull
    private static List<XmlTag> getGlobalElementsInScope(Project project, GlobalSearchScope searchScope) {
        final List<XmlTag> result = new ArrayList<>();
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(StdFileTypes.XML, searchScope);
        final DomManager manager = DomManager.getDomManager(project);
        for (VirtualFile file : files) {
            final PsiFile xmlFile = PsiManager.getInstance(project).findFile(file);
            if (isMuleFile(xmlFile)) {
                final DomFileElement<Mule> fileElement = manager.getFileElement((XmlFile) xmlFile, Mule.class);
                if (fileElement != null) {
                    final Mule rootElement = fileElement.getRootElement();
                    final XmlTag[] subTags = rootElement.getXmlTag().getSubTags();
                    for (XmlTag subTag : subTags) {
                        if (isGlobalElement(subTag)) {
                            result.add(subTag);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static boolean isGlobalElement(XmlTag subTag) {
        return !(subTag.getName().equals("flow") || subTag.getName().equals("sub-flow") || subTag.getLocalName().equals("test"));
    }

    @Nullable
    public static XmlTag findParentXmlTag(PsiElement element) {
        PsiElement psiElement = element;

        while (psiElement != null && !(psiElement instanceof XmlTag))
            psiElement = psiElement.getParent();

        return (XmlTag) psiElement;
    }

    public static boolean isMuleProject(Project project) {
        ProjectFacetManager manager = ProjectFacetManager.getInstance(project);
        final List<MuleFacet> facets = manager.getFacets(MuleFacetType.TYPE_ID);
        return (facets != null && !facets.isEmpty());
    }

    public static boolean isMuleModule(Module module) {
        boolean muleModule = false;
        ProjectFacetManager manager = ProjectFacetManager.getInstance(module.getProject());
        final List<MuleFacet> facets = manager.getFacets(MuleFacetType.TYPE_ID, new Module[]{module});
        muleModule = (facets != null && !facets.isEmpty());
        if (!muleModule) { //Check for src/main/apps too, since not all modules may have facet added
            VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
            for (VirtualFile f : contentRoots) {
                VirtualFile deployProperties = f.findFileByRelativePath("src/main/app/mule-deploy.properties");
                if (deployProperties != null && deployProperties.exists() && deployProperties.isValid())
                    return true;
            }
        }
        return muleModule;
    }

    public static boolean isMule4Module(Module module) {
        boolean mule4Module = false;

        ProjectFacetManager manager = ProjectFacetManager.getInstance(module.getProject());
        final List<MuleFacet> facets = manager.getFacets(MuleFacetType.TYPE_ID, new Module[]{module});

        if (facets != null && !facets.isEmpty()) {
            for (MuleFacet nextFacet : facets) {
                MuleFacetConfiguration configuration = nextFacet.getConfiguration();
                if (!(StringUtils.isEmpty(configuration.getPathToSdk()))) {
                    MuleSdk sdk = new MuleSdk(configuration.getPathToSdk());
                    String sdkVersion = sdk.getVersion();
                    if (sdkVersion.startsWith("4"))
                        mule4Module = true;
                }
            }
        }

        if (!mule4Module) { //Check for src/main/apps too, since not all modules may have facet added
            VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
            for (VirtualFile f : contentRoots) {
                VirtualFile deployProperties = f.findFileByRelativePath("mule-artifact.json");
                if (deployProperties != null && deployProperties.exists() && deployProperties.isValid())
                    return true;
            }
        }
        return mule4Module;
    }

    //TODO - Add isMule4Project and isMule4Module

    public static boolean isMuleDomainModule(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        for (VirtualFile f : contentRoots) {
            VirtualFile domainConfig = f.findFileByRelativePath("src/main/domain/mule-domain-config.xml");
            if (domainConfig != null && domainConfig.exists() && domainConfig.isValid())
                return true;
        }
        return false;
    }

    public static List<XmlTag> findFlowRefsForFlow(@NotNull XmlTag flow) {
        List<XmlTag> flowRefs = new ArrayList<>();

        final Project project = flow.getProject();
        final String flowName = flow.getAttributeValue(MuleConfigConstants.NAME_ATTRIBUTE);

        Collection<VirtualFile> vFiles = FileTypeIndex.getFiles(StdFileTypes.XML, ProjectScope.getContentScope(project));
        for (VirtualFile virtualFile : vFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile != null) {
                XmlFile xmlFile = (XmlFile) psiFile;
                XmlTag mule = xmlFile.getRootTag();

                FlowRefsFinder finder = new FlowRefsFinder(flowName);
                mule.accept(finder);
                flowRefs.addAll(finder.getFlowRefs());
            }
        }
        return flowRefs;
    }

    public static List<Module> getMuleModules(Project project, boolean includeDomains)
    {
        final List<Module> muleModules = new ArrayList<>();

        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] allModules = moduleManager.getModules();
        for (Module m : allModules) {
            if (MuleConfigUtils.isMuleModule(m) || (includeDomains && MuleConfigUtils.isMuleDomainModule(m)))
                muleModules.add(m);
        }
        return muleModules;
    }
    
    private static class FlowRefsFinder extends PsiRecursiveElementVisitor {
        private List<XmlTag> flowRefs = new ArrayList<>();
        private String flowName;

        public FlowRefsFinder(@NotNull String flowName) {
            this.flowName = flowName;
        }

        public void visitElement(PsiElement element) {
            super.visitElement(element);

            if (element != null && element instanceof XmlTag) {
                XmlTag tag = (XmlTag) element;
                if (MuleConfigConstants.FLOW_REF_TAG_NAME.equals(tag.getName())) {
                    String fn = tag.getAttributeValue(MuleConfigConstants.NAME_ATTRIBUTE);
                    if (flowName.equals(fn))
                        flowRefs.add(tag);
                }
            }
        }

        public List<XmlTag> getFlowRefs() {
            return flowRefs;
        }
    }
}
