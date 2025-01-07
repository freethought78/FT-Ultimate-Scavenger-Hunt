package FTUltimateScavengerHunt;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class ScavengerHuntPanel extends Screen {
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogUtils.getLogger();

    private String playerName;
    private Map<String, Boolean> playerProgress;
    private final int columns = 3;  // You can adjust this number based on the screen width

    private int scrollOffset = 0;  // To track vertical scrolling
    private int contentHeight = 0; // To track the total height of all the items
    private int maxScrollOffset = 0;  // Maximum scrollable offset, for when the content overflows
    private static final int SCROLL_STEP = 20; // How much to scroll per step

    // Panel margins
    private static final int MARGIN_TOP = 20;  // 5% of the screen height for top margin
    private static final int MARGIN_SIDES = 20;  // 5% of the screen width for side margins
    private static final int MARGIN_BOTTOM = 40;  // Adjust for the bottom margin to avoid hotbar overlap

    // Panel size based on screen dimensions and margins
    private int panelWidth;
    private int panelHeight;

    @SuppressWarnings("resource")
    public ScavengerHuntPanel() {
        super(new TextComponent("Scavenger Hunt Progress"));
        this.playerName = Minecraft.getInstance().player.getName().getString();  // Get the player's UUID
        this.playerProgress = PlayerProgressManager.masterPlayerProgress.get(playerName);  // Get the player's progress
    }

    @Override
    protected void init() {
        // Calculate panel width and height based on margins and screen size
        panelWidth = this.width - (2 * MARGIN_SIDES);
        panelHeight = this.height - (MARGIN_TOP + MARGIN_BOTTOM);

        // Add a close button at the top-right corner
        this.addRenderableWidget(new Button(this.width - MARGIN_SIDES - 25, MARGIN_TOP + 5, 20, 20, new TextComponent("X"), button -> {
            this.onClose(); // Close the screen when the button is pressed
        }));
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        // Draw a semi-transparent background for the entire panel
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.8F); // Black panel background with 80% opacity
        fill(poseStack, MARGIN_SIDES, MARGIN_TOP, MARGIN_SIDES + panelWidth, MARGIN_TOP + panelHeight, 0xFFFFFFFF); 
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        int screenWidth = Minecraft.getInstance().getWindow().getWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getHeight();

        // Normalize the coordinates and size based on screen resolution
        float normalizedX = (float) MARGIN_SIDES / this.width;
        float normalizedY = (float) (this.height - MARGIN_BOTTOM) / this.height;
        normalizedY = Math.abs(1 - normalizedY);
        float normalizedWidth = (float) panelWidth / this.width;
        float normalizedHeight = (float) panelHeight / this.height;

        // Now scale them to the actual screen resolution
        int scissorX = (int) (normalizedX * screenWidth);
        int scissorY = (int) (normalizedY * screenHeight);  // Flip Y coordinate
        int scissorWidth = (int) (normalizedWidth * screenWidth);
        int scissorHeight = (int) (normalizedHeight * screenHeight);

        // Enable the scissor test with the correct screen-space coordinates
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        // Render the background
        this.renderBackground(poseStack);

        // Title rendering (no change)
        int textWidth = this.font.width("Scavenger Hunt Progress");
        int titleX = (this.width / 2) - (textWidth / 2);
        int titleY = MARGIN_TOP + 10 - scrollOffset;
        this.font.draw(poseStack, new TextComponent("Scavenger Hunt Progress"), titleX, titleY, 0xFFFFFF);

        // Split the items into complete and incomplete lists (no change)
        List<String> incompleteItems = new ArrayList<>();
        List<String> completeItems = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : playerProgress.entrySet()) {
            if (entry.getValue()) {
                completeItems.add(entry.getKey());
            } else {
                incompleteItems.add(entry.getKey());
            }
        }

        // Render Incomplete Items Header (no change)
        int yOffset = MARGIN_TOP + 40;
        this.font.draw(poseStack, new TextComponent("Incomplete Items"), MARGIN_SIDES, yOffset - scrollOffset, 0xFFFF00);
        yOffset += 20;

        // Render incomplete items (no change)
        drawItemsInColumns(poseStack, incompleteItems, yOffset, columns, 16, 5, 0xFF0000);

        // Calculate content height (no change)
        contentHeight = yOffset + (incompleteItems.size() / columns + 1) * 16 + 10;

        // Render Complete Items Header (no change)
        yOffset += (incompleteItems.size() / columns + 1) * 16 + 10;
        this.font.draw(poseStack, new TextComponent("Complete Items"), MARGIN_SIDES, yOffset - scrollOffset, 0xFFFF00);
        yOffset += 20;

        // Render complete items (no change)
        drawItemsInColumns(poseStack, completeItems, yOffset, columns, 16, 5, 0x00FF00);

        // Calculate content height (no change)
        contentHeight = yOffset + (completeItems.size() / columns + 1) * 16 + 10;

        // Fetch and render the leaderboard data
        List<LeaderboardManager.LeaderboardEntry> leaderboard = LeaderboardManager.getLeaderboard();
        
        String huntWinner = FTUltimateScavengerHunt.huntWinner;
        String winnerMessage;
        
        if (huntWinner == null) {
        	winnerMessage = "No winner yet.";
        } else {
        	winnerMessage = huntWinner + " IS THE WINNER!";
        }
        
        yOffset += (completeItems.size() / columns + 1) * 16 + 10;  // Adjust for the complete items
        this.font.draw(poseStack, new TextComponent("Leaderboard - " + playerProgress.size() + " items total - " + winnerMessage), MARGIN_SIDES, yOffset - scrollOffset, 0xFFFF00); // Yellow header
        yOffset += 20; // Space for header

        // Render leaderboard entries (Username and Completion Count)
        for (LeaderboardManager.LeaderboardEntry entry : leaderboard) {
            String leaderboardText = entry.playerName + ": " + entry.completionCount;
            this.font.draw(poseStack, new TextComponent(leaderboardText), MARGIN_SIDES, yOffset - scrollOffset, 0xFFFFFF);
            yOffset += 16; // Increase yOffset for each entry
        }

        // Update content height with leaderboard
        contentHeight = yOffset;

        // Ensure the content doesn't scroll past the bottom (no change)
        maxScrollOffset = Math.max(contentHeight - panelHeight + 40, 0);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        // Render the scrollbar if necessary (no change)
        renderScrollbar(poseStack);

        RenderSystem.disableScissor();
    }


    // Method to render items in columns with color
    private void drawItemsInColumns(PoseStack poseStack, List<String> items, int yOffset, int columns, int lineHeight, int spacing, int color) {
        int columnWidth = panelWidth / columns;  // Adjust width based on the number of columns
        int currentColumn = 0;
        int currentRow = 0;

        for (String item : items) {
            int x = MARGIN_SIDES + currentColumn * columnWidth;
            int y = yOffset + currentRow * lineHeight - scrollOffset;  // Apply scroll offset

            // Remove brackets around the item name (use regular expression)
            String itemName = item.replaceAll("[\\[\\]]", "");

            // Render each item with the appropriate color
            this.font.draw(poseStack, new TextComponent(itemName), x, y, color);  // Directly using String

            // Update row and column
            currentRow++;
            if (currentRow >= items.size() / columns + 1) {
                currentRow = 0;
                currentColumn++;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            // Scroll up
            scrollOffset = Math.max(scrollOffset - SCROLL_STEP, 0);
        } else {
            // Scroll down
            scrollOffset = Math.min(scrollOffset + SCROLL_STEP, maxScrollOffset);
        }
        return true;
    }

    private void renderScrollbar(PoseStack poseStack) {
        if (maxScrollOffset > 0) {
            int scrollbarHeight = panelHeight;  // Total height of the scrollable area
            int scrollbarY = MARGIN_TOP;  // Start position of the scrollbar (just below the title)
            int thumbHeight = Math.max(scrollbarHeight * 20 / contentHeight, 20); // Ensure the thumb is visible
            
            // Calculate the position of the scrollbar thumb
            int scrollAmount = (int)((double)scrollOffset / maxScrollOffset * (scrollbarHeight - thumbHeight)); // Ensure thumb doesn't exceed bounds
            
            // Clamp the thumb's scroll position to ensure it stays within the scrollbar
            scrollAmount = Math.max(0, Math.min(scrollAmount, scrollbarHeight - thumbHeight));

            // Draw the scrollbar background
            fill(poseStack, this.width - MARGIN_SIDES - 15, scrollbarY, this.width - MARGIN_SIDES - 5, scrollbarY + scrollbarHeight, 0x80000000);  // Semi-transparent background

            // Draw the scrollbar "thumb" (the draggable part)
            fill(poseStack, this.width - MARGIN_SIDES - 15, scrollbarY + scrollAmount, this.width - MARGIN_SIDES - 5, scrollbarY + scrollAmount + thumbHeight, 0xFFFFFFFF);  // White thumb
        }
    }


    @Override
    public boolean isPauseScreen() {
        return false; // Allows the game to keep running while the panel is open
    }

    @Override
    public void onClose() {
        // This will be called when the screen is closed
        this.minecraft.setScreen(null);
    }
}
