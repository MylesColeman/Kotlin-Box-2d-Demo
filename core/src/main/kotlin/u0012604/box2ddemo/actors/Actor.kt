package u0012604.box2ddemo.actors

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.Disposable

interface Actor : Disposable {
    val world: World
    val body: Body
    val sprite: Sprite

    fun update()
}
