package com.zdy.project.wechat_chatroom_helper.plugins.main.adapter

import android.widget.ImageView
import com.zdy.project.wechat_chatroom_helper.ChatInfoModel
import com.zdy.project.wechat_chatroom_helper.LogUtils
import com.zdy.project.wechat_chatroom_helper.plugins.PluginEntry
import com.zdy.project.wechat_chatroom_helper.wechat.WXObject
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.ParameterizedType

object ConversationItemHandler {

    private val conversationWithCacheAdapter = XposedHelpers.findClass(WXObject.Adapter.C.ConversationWithCacheAdapter, PluginEntry.classloader)
    private val conversationAvatar = XposedHelpers.findClass(WXObject.Adapter.C.ConversationAvatar, PluginEntry.classloader)

    private val conversationTimeStringMethod = conversationWithCacheAdapter.declaredMethods
            .filter { !it.isAccessible }.filter { it.returnType == CharSequence::class.java }
            .first { it.parameterTypes.size == 1 }

    private val conversationAvatarMethod = conversationAvatar.methods
            .first { it.parameterTypes.isNotEmpty() && it.parameterTypes[0].name == ImageView::class.java.name }

    private val beanClass = ((conversationWithCacheAdapter.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<*>)

    fun getConversationTimeString(adapter: Any, conversationTime: Long): CharSequence {

        conversationTimeStringMethod.let {

            val constructor = beanClass.constructors.filter { it.parameterTypes.size == 1 }.firstOrNull { it.parameterTypes[0] == String::class.java }

            constructor?.let {
                val obj = constructor.newInstance("")

                beanClass.getField("field_status").set(obj, 0)
                beanClass.getField("field_conversationTime").set(obj, conversationTime)

                return XposedHelpers.callMethod(adapter, conversationTimeStringMethod.name, obj) as CharSequence
            }
        }
        return ""
    }

    fun getConversationAvatar(field_username: String, imageView: ImageView) =
            XposedHelpers.callStaticMethod(conversationAvatar, conversationAvatarMethod.name, imageView, field_username)


    fun getConversationContent(adapter: Any, chatInfoModel: ChatInfoModel, position: Int): CharSequence? {

        val getContentMethod = conversationWithCacheAdapter.declaredMethods
                .filter { it.parameterTypes.size == 3 }
                .single {
                    it.parameterTypes[0].simpleName == beanClass.simpleName &&
                            it.parameterTypes[1].simpleName == Int::class.java.simpleName &&
                            it.parameterTypes[2].simpleName == Boolean::class.java.simpleName
                }

        val aeConstructor = beanClass.constructors.filter { it.parameterTypes.size == 1 }.firstOrNull { it.parameterTypes[0] == String::class.java }!!
        val ae = aeConstructor.newInstance(chatInfoModel.username)

        beanClass.getField("field_editingMsg").set(ae, chatInfoModel.editingMsg)
        beanClass.getField("field_atCount").set(ae, chatInfoModel.atCount)
        beanClass.getField("field_unReadCount").set(ae, chatInfoModel.unReadCount)
        beanClass.getField("field_unReadMuteCount").set(ae, chatInfoModel.unReadMuteCount)
        beanClass.getField("field_msgType").set(ae, chatInfoModel.msgType)
        beanClass.getField("field_username").set(ae, chatInfoModel.username)
        beanClass.getField("field_content").set(ae, chatInfoModel.content)
        beanClass.getField("field_digest").set(ae, chatInfoModel.digest)
        beanClass.getField("field_digestUser").set(ae, chatInfoModel.digestUser)
        beanClass.getField("field_isSend").set(ae, chatInfoModel.isSend)
        beanClass.getField("field_UnReadInvite").set(ae, chatInfoModel.UnReadInvite)
        beanClass.getField("field_atCount").set(ae, chatInfoModel.atCount)

        val content = XposedHelpers.callMethod(adapter, getContentMethod.name, ae, position, false) as CharSequence

        LogUtils.log("getConversationContent,  content =  $content")

        return if (content.isEmpty()) null else content
    }

}