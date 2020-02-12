package one.mixin.android.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.util.QueryMessage
import one.mixin.android.util.Session
import one.mixin.android.vo.HyperlinkItem
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem

@Dao
interface MessageDao : BaseDao<Message> {
    companion object {
        const val PREFIX_MESSAGE_ITEM =
            """
                SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
                u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type,
                m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform,
                m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight,
                m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, m.quote_message_id as quoteId,
                m.quote_content as quoteContent, u1.full_name AS participantFullName, m.action AS actionName, u1.user_id AS participantUserId,
                s.snapshot_id AS snapshotId, s.type AS snapshotType, s.amount AS snapshotAmount, a.symbol AS assetSymbol, s.asset_id AS assetId,
                a.icon_url AS assetIcon, st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId,
                st.name AS assetName, st.asset_type AS assetType, h.site_name AS siteName, h.site_title AS siteTitle, h.site_description AS siteDescription,
                h.site_image AS siteImage, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber,
                su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId,
                c.name AS groupName
                FROM messages m
                INNER JOIN users u ON m.user_id = u.user_id
                LEFT JOIN users u1 ON m.participant_id = u1.user_id
                LEFT JOIN snapshots s ON m.snapshot_id = s.snapshot_id
                LEFT JOIN assets a ON s.asset_id = a.asset_id
                LEFT JOIN stickers st ON st.sticker_id = m.sticker_id
                LEFT JOIN hyperlinks h ON m.hyperlink = h.hyperlink
                LEFT JOIN users su ON m.shared_user_id = su.user_id
                LEFT JOIN conversations c ON m.conversation_id = c.conversation_id
                WHERE m.conversation_id = :conversationId 
            """
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_MESSAGE_ITEM ORDER BY m.created_at DESC")
    fun getMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query(
        """SELECT count(*) FROM messages WHERE conversation_id = :conversationId
            AND rowid > (SELECT rowid FROM messages WHERE id = :messageId)"""
    )
    suspend fun findMessageIndex(conversationId: String, messageId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id WHERE m.conversation_id = :conversationId
        AND (m.category = 'SIGNAL_IMAGE' OR m.category = 'PLAIN_IMAGE' OR m.category = 'SIGNAL_VIDEO' OR m.category = 'PLAIN_VIDEO'
        OR m.category = 'SIGNAL_LIVE' OR m.category = 'PLAIN_LIVE') 
        ORDER BY m.created_at ASC
        """
    )
    fun getMediaMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m 
        INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND m.id = :messageId
        """
    )
    suspend fun getMediaMessage(conversationId: String, messageId: String): MessageItem

    @Query(
        """SELECT count(*) FROM messages WHERE conversation_id = :conversationId
        AND rowid < (SELECT rowid FROM messages WHERE id = :messageId)
        AND (category = 'SIGNAL_IMAGE' OR category = 'PLAIN_IMAGE' OR category = 'SIGNAL_VIDEO' OR category = 'PLAIN_VIDEO'
        OR category = 'SIGNAL_LIVE' OR category = 'PLAIN_LIVE')
        ORDER BY created_at ASC
        """
    )
    suspend fun indexMediaMessages(conversationId: String, messageId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id
        WHERE m.conversation_id = :conversationId
        AND (m.category = 'SIGNAL_IMAGE' OR m.category = 'PLAIN_IMAGE' OR m.category = 'SIGNAL_VIDEO' OR m.category = 'PLAIN_VIDEO')
        ORDER BY m.created_at DESC
        """
    )
    fun getMediaMessagesExcludeLive(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query(
        """SELECT count(*) FROM messages WHERE conversation_id = :conversationId 
        AND rowid > (SELECT rowid FROM messages WHERE id = :messageId)
        AND (category = 'SIGNAL_IMAGE' OR category = 'PLAIN_IMAGE' OR category = 'SIGNAL_VIDEO' OR category = 'PLAIN_VIDEO')
        ORDER BY created_at DESC
        """
    )
    suspend fun indexMediaMessagesExcludeLive(conversationId: String, messageId: String): Int

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, u.avatar_url AS userAvatarUrl,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_width AS mediaWidth, m.media_height AS mediaHeight, m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.media_duration AS mediaDuration,  m.media_waveform AS mediaWaveform
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND (m.category = 'SIGNAL_AUDIO' OR m.category = 'PLAIN_AUDIO')
        ORDER BY m.created_at DESC
        """
    )
    fun getAudioMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @Query(
        """
            SELECT h.hyperlink AS hyperlink, h.site_description AS siteDescription, h.site_image AS siteImage,
            h.site_name AS siteName, h.site_title AS siteTitle, m.created_at AS createdAt
            FROM hyperlinks h INNER JOIN messages m ON h.hyperlink = m.hyperlink
            WHERE m.conversation_id = :conversationId
            AND (m.category = 'SIGNAL_TEXT' OR m.category = 'PLAIN_TEXT')
            ORDER BY m.created_at DESC
        """
    )
    fun getLinkMessages(conversationId: String): DataSource.Factory<Int, HyperlinkItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId,
        u.full_name AS userFullName, u.identity_number AS userIdentityNumber, m.category AS type,
        m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus,
        m.media_url AS mediaUrl, m.media_mime_type AS mediaMimeType, m.name AS mediaName, m.media_size AS mediaSize
        FROM messages m INNER JOIN users u ON m.user_id = u.user_id 
        WHERE m.conversation_id = :conversationId
        AND (m.category = 'SIGNAL_DATA' OR m.category = 'PLAIN_DATA')
        ORDER BY m.created_at DESC
        """
    )
    fun getFileMessages(conversationId: String): DataSource.Factory<Int, MessageItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT m.id AS messageId, m.conversation_id AS conversationId, u.user_id AS userId, " +
            "u.full_name AS userFullName, u.identity_number AS userIdentityNumber, u.app_id AS appId, m.category AS type, " +
            "m.content AS content, m.created_at AS createdAt, m.status AS status, m.media_status AS mediaStatus, m.media_waveform AS mediaWaveform, " +
            "m.name AS mediaName, m.media_mime_type AS mediaMimeType, m.media_size AS mediaSize, m.media_width AS mediaWidth, m.media_height AS mediaHeight, " +
            "m.thumb_image AS thumbImage, m.thumb_url AS thumbUrl, m.media_url AS mediaUrl, m.media_duration AS mediaDuration, " +
            "m.quote_message_id as quoteId, m.quote_content as quoteContent, " +
            "st.asset_url AS assetUrl, st.asset_width AS assetWidth, st.asset_height AS assetHeight, st.sticker_id AS stickerId, " +
            "st.name AS assetName, st.asset_type AS assetType, m.shared_user_id AS sharedUserId, su.full_name AS sharedUserFullName, su.identity_number AS sharedUserIdentityNumber, " +
            "su.avatar_url AS sharedUserAvatarUrl, su.is_verified AS sharedUserIsVerified, su.app_id AS sharedUserAppId " +
            "FROM messages m " +
            "INNER JOIN users u ON m.user_id = u.user_id " +
            "LEFT JOIN stickers st ON st.sticker_id = m.sticker_id " +
            "LEFT JOIN users su ON m.shared_user_id = su.user_id " +
            "WHERE m.conversation_id = :conversationId AND m.id = :messageId AND m.status != 'FAILED'"
    )
    fun findMessageItemById(conversationId: String, messageId: String): QuoteMessageItem?

    @Query("SELECT count(id) FROM messages WHERE conversation_id = :conversationId AND quote_message_id = :messageId AND quote_content IS NULL")
    fun countMessageByQuoteId(conversationId: String, messageId: String): Int

    @Query("UPDATE messages SET quote_content = :content WHERE conversation_id = :conversationId AND quote_message_id = :messageId")
    fun updateQuoteContentByQuoteId(conversationId: String, messageId: String, content: String)

    @Query(
        """
            SELECT m.conversation_id AS conversationId, c.icon_url AS conversationAvatarUrl,
            c.name AS conversationName, c.category AS conversationCategory, count(m.id) as messageCount,
            u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName
            FROM messages_fts4 mf4 
            INNER JOIN users u ON c.owner_id = u.user_id
            INNER JOIN conversations c ON c.conversation_id = m.conversation_id
            INNER JOIN messages m ON m.id = mf4.message_id
            WHERE messages_fts4 MATCH :query
            AND m.status != 'FAILED' 
            AND m.category IN('SIGNAL_TEXT', 'PLAIN_TEXT', 'SIGNAL_DATA', 'PLAIN_DATA', 'SIGNAL_POST', 'PLAIN_POST') 
            GROUP BY m.conversation_id
            ORDER BY m.created_at DESC
            LIMIT :limit
        """
    )
    suspend fun fuzzySearchMessage(query: String, limit: Int): List<SearchMessageItem>

    @Query(
        """
            SELECT m.id AS messageId, u.user_id AS userId, u.avatar_url AS userAvatarUrl, u.full_name AS userFullName,
            m.category AS type, m.content AS content, m.created_at AS createdAt, m.name AS mediaName 
            FROM messages_fts4 mf4 
            INNER JOIN users u ON m.user_id = u.user_id 
            INNER JOIN messages m ON m.id = mf4.message_id
            WHERE messages_fts4 MATCH :query
            AND m.status != 'FAILED' 
            AND m.category IN ('SIGNAL_TEXT', 'PLAIN_TEXT', 'SIGNAL_DATA', 'PLAIN_DATA', 'SIGNAL_POST', 'PLAIN_POST') 
            AND m.conversation_id = :conversationId
            ORDER BY m.created_at DESC
        """
    )
    fun fuzzySearchMessageByConversationId(
        query: String,
        conversationId: String
    ): DataSource.Factory<Int, SearchMessageDetailItem>

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteMessageByConversationId(conversationId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    fun updateMessageStatus(status: String, id: String)

    @Query("UPDATE messages SET status = 'SENT' WHERE id = :id AND status = 'FAILED'")
    fun recallFailedMessage(id: String)

    @Query(
        "UPDATE messages SET category = 'MESSAGE_RECALL', content = NULL, media_url = NULL, media_mime_type = NULL, media_size = NULL, " +
            "media_duration = NULL, media_width = NULL, media_height = NULL, media_hash = NULL, thumb_image = NULL, media_key = NULL, " +
            "media_digest = NUll, media_status = NULL, action = NULL, participant_id = NULL, snapshot_id = NULL, hyperlink = NULL, name = NULL, " +
            "album_id = NULL, sticker_id = NULL, shared_user_id = NULL, media_waveform = NULL, quote_message_id = NULL, quote_content = NULL WHERE id = :id"
    )
    fun recallMessage(id: String)

    @Query("UPDATE messages SET media_status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaStatus(status: String, id: String)

    @Query("UPDATE messages SET media_status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    suspend fun updateMediaStatusSuspend(status: String, id: String)

    @Query("UPDATE messages SET media_size = :mediaSize WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaSize(mediaSize: Long, id: String)

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMediaMessageUrl(mediaUrl: String, id: String)

    @Query("UPDATE messages SET media_url = :mediaUrl WHERE media_url = :oldMediaUrl AND category != 'MESSAGE_RECALL'")
    fun updateMediaUrl(mediaUrl: String, oldMediaUrl: String)

    @Query("UPDATE messages SET hyperlink = :hyperlink WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateHyperlink(hyperlink: String, id: String)

    @Query(
        "SELECT id, conversation_id, user_id, status, created_at FROM messages WHERE conversation_id = :conversationId AND user_id != :userId " +
            "AND status IN ('SENT', 'DELIVERED') ORDER BY created_at ASC"
    )
    fun getUnreadMessage(conversationId: String, userId: String): List<MessageMinimal>

    @Query(
        "UPDATE messages SET content = :content, media_mime_type = :mediaMimeType, " +
            "media_size = :mediaSize, media_width = :mediaWidth, media_height = :mediaHeight, " +
            "thumb_image = :thumbImage, media_key = :mediaKey, media_digest = :mediaDigest, media_duration = :mediaDuration, " +
            "media_status = :mediaStatus, status = :status, name = :name, media_waveform = :mediaWaveform WHERE id = :messageId " +
            "AND category != 'MESSAGE_RECALL'"
    )
    fun updateAttachmentMessage(
        messageId: String,
        content: String,
        mediaMimeType: String,
        mediaSize: Long,
        mediaWidth: Int?,
        mediaHeight: Int?,
        thumbImage: String?,
        name: String?,
        mediaWaveform: ByteArray?,
        mediaDuration: String?,
        mediaKey: ByteArray?,
        mediaDigest: ByteArray?,
        mediaStatus: String,
        status: String
    )

    @Query("UPDATE messages SET sticker_id = :stickerId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateStickerMessage(stickerId: String, status: String, messageId: String)

    @Query("UPDATE messages SET shared_user_id = :sharedUserId, status = :status WHERE id = :messageId AND category != 'MESSAGE_RECALL'")
    fun updateContactMessage(sharedUserId: String, status: String, messageId: String)

    @Query(
        "UPDATE messages SET media_width = :width, media_height = :height, media_url=:url, thumb_url = :thumbUrl, status = :status " +
            "WHERE id = :messageId AND category != 'SIGNAL_LIVE'"
    )
    fun updateLiveMessage(
        width: Int,
        height: Int,
        url: String,
        thumbUrl: String,
        status: String,
        messageId: String
    )

    @Query("UPDATE messages SET content = :content, status = :status WHERE id = :id AND category != 'MESSAGE_RECALL'")
    fun updateMessageContentAndStatus(content: String, status: String, id: String)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    fun updateMessageContent(content: String, id: String)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun findMessageById(messageId: String): Message?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun suspendFindMessageById(messageId: String): Message?

    @Query("SELECT status FROM messages WHERE id = :messageId")
    fun findMessageStatusById(messageId: String): String?

    // id not null means message exists
    @Query("SELECT id FROM messages WHERE id = :messageId")
    fun findMessageIdById(messageId: String): String?

    @Query("SELECT id, conversation_id, user_id, status, created_at FROM messages WHERE id = :messageId")
    fun findSimpleMessageById(messageId: String): MessageMinimal?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT id, conversation_id, user_id, status, created_at FROM messages WHERE conversation_id = :conversationId " +
            "AND user_id != :userId AND status IN ('SENT', 'DELIVERED') ORDER BY created_at ASC"
    )
    fun findUnreadMessagesSync(
        conversationId: String,
        userId: String = Session.getAccountId()!!
    ): List<MessageMinimal>?

    @Query(
        "SELECT id FROM messages WHERE conversation_id = :conversationId AND user_id = :userId AND " +
            "status = 'FAILED' ORDER BY created_at DESC LIMIT 1000"
    )
    fun findFailedMessages(conversationId: String, userId: String): List<String>

    @Query(
        "SELECT m.id as messageId, m.media_url as mediaUrl FROM messages m WHERE conversation_id = :conversationId " +
            "AND category = :category ORDER BY created_at ASC"
    )
    fun getMediaByConversationIdAndCategory(
        conversationId: String,
        category: String
    ): List<MediaMessageMinimal>?

    @Query(
        "UPDATE messages SET status = 'READ' WHERE conversation_id = :conversationId AND user_id != :userId " +
            "AND status IN ('SENT', 'DELIVERED') AND created_at <= :createdAt"
    )
    fun batchMarkRead(conversationId: String, userId: String, createdAt: String)

    @Query(
        "UPDATE conversations SET unseen_message_count = (SELECT count(1) FROM messages m WHERE m.conversation_id = :conversationId AND m.user_id != :userId " +
            "AND m.status IN ('SENT', 'DELIVERED')) WHERE conversation_id = :conversationId "
    )
    fun takeUnseen(userId: String, conversationId: String)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "$PREFIX_MESSAGE_ITEM AND (m.category = 'SIGNAL_AUDIO' OR m.category = 'PLAIN_AUDIO') AND m.created_at >= :createdAt AND " +
            "m.rowid > (SELECT rowid FROM messages WHERE id = :messageId) LIMIT 1"
    )
    suspend fun findNextAudioMessageItem(
        conversationId: String,
        createdAt: String,
        messageId: String
    ): MessageItem?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId AND (category = 'SIGNAL_AUDIO' OR category = 'PLAIN_AUDIO') " +
            "AND created_at >= :createdAt AND rowid > (SELECT rowid FROM messages WHERE id = :messageId) LIMIT 1"
    )
    suspend fun findNextAudioMessage(
        conversationId: String,
        createdAt: String,
        messageId: String
    ): Message?

    @Query("""
        SELECT id FROM messages WHERE conversation_id =:conversationId ORDER BY created_at DESC, rowid DESC LIMIT 1 OFFSET :offset
        """
    )
    suspend fun findFirstUnreadMessageId(conversationId: String, offset: Int): String?

    @Query("SELECT id FROM messages WHERE conversation_id =:conversationId ORDER BY created_at DESC LIMIT 1")
    suspend fun findLastMessage(conversationId: String): String?

    @Query(
        "SELECT id FROM messages WHERE conversation_id =:conversationId AND user_id !=:userId AND messages.rowid > " +
            "(SELECT rowid FROM messages WHERE id = :messageId) ORDER BY rowid ASC LIMIT 1"
    )
    suspend fun findUnreadMessageByMessageId(
        conversationId: String,
        userId: String,
        messageId: String
    ): String?

    @Query("SELECT count(id) FROM messages WHERE conversation_id =:conversationId AND user_id =:userId")
    suspend fun isSilence(conversationId: String, userId: String): Int

    @Query("SELECT * FROM messages WHERE id IN (:messageIds) ORDER BY created_at, rowid")
    suspend fun getSortMessagesByIds(messageIds: List<String>): List<Message>

    @Query("""
        SELECT id as message_id, content, name FROM messages 
        WHERE category IN ('PLAIN_TEXT', 'SIGNAL_TEXT', 'PLAIN_DATA', 'SIGNAL_DATA') 
        LIMIT :limit OFFSET :offset
        """)
    suspend fun batchQueryMessages(limit: Int, offset: Int): List<QueryMessage>
}
