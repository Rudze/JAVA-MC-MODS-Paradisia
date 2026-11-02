package tallestred.numismaticoverhaul.client.gui.purse;

import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;

import java.util.List;

public class PurseLayerContainer extends StackLayout {

    public PurseLayerContainer(Sizing horizontalSizing, Sizing verticalSizing) {
        super(horizontalSizing, verticalSizing);
    }

    @Override
    protected void drawChildren(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta, List<? extends Component> children) {
        context.getMatrixStack().pushPose();
        context.getMatrixStack().translate(0, 0, 300);
        super.drawChildren(context, mouseX, mouseY, partialTicks, delta, children);
        context.getMatrixStack().popPose();
    }
}
