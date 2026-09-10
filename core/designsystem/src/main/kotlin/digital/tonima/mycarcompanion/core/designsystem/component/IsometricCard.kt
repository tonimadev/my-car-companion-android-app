package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.core.designsystem.util.isometricDepth

@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    depthColor: Color = MaterialTheme.colorScheme.outlineVariant,
    depth: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(bottom = depth, end = depth)
            .isometricDepth(depth = depth, color = depthColor)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IsometricCardPreview() {
    MyCarCompanionTheme {
        Box(Modifier.padding(24.dp)) {
            IsometricCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ) {
                Text("Isometric Content")
                Text("Matches the car view style")
            }
        }
    }
}
