import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import {
  ActionSheetIOS, ActivityIndicator, Image,
  KeyboardAvoidingView, Modal, Platform,
  SafeAreaView, ScrollView, StyleSheet, Text,
  TextInput, TouchableOpacity, View
} from 'react-native';
import { supabase } from '../../services/supabase';

export default function TripScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams();
  const scrollRef = useRef<ScrollView>(null);

  const [trip, setTrip] = useState<any>(null);
  const [author, setAuthor] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [likeLoading, setLikeLoading] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);

  // Comentarios
  const [comments, setComments] = useState<any[]>([]);
  const [commentText, setCommentText] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  // Modales
  const [menuVisible, setMenuVisible] = useState(false);
  const [confirmDeleteTrip, setConfirmDeleteTrip] = useState(false);
  const [confirmDeleteCommentId, setConfirmDeleteCommentId] = useState<string | null>(null);

  useEffect(() => { loadTrip(); }, [id]);

  async function loadTrip() {
    const { data, error } = await supabase
      .from('trips').select('*').eq('id', id).single();
    if (error || !data) { router.back(); return; }
    setTrip(data);

    const { data: userData } = await supabase
      .from('users').select('*').eq('id', data.user_id).single();
    setAuthor(userData);

    const { data: session } = await supabase.auth.getSession();
    const userId = session?.session?.user?.id ?? null;
    setCurrentUserId(userId);

    const { count } = await supabase
      .from('trip_likes').select('*', { count: 'exact', head: true }).eq('trip_id', id);
    setLikeCount(count ?? 0);

    if (userId) {
      const { data: myLike } = await supabase
        .from('trip_likes').select('id').eq('trip_id', id).eq('user_id', userId).maybeSingle();
      setLiked(!!myLike);
    }

    await loadComments();
    setLoading(false);
  }

  async function loadComments() {
    const { data } = await supabase
      .from('trip_comments')
      .select('id, content, created_at, user_id, users(username)')
      .eq('trip_id', id)
      .order('created_at', { ascending: true });
    setComments(data || []);
  }

  async function toggleLike() {
    if (likeLoading || !currentUserId) return;
    setLikeLoading(true);
    if (liked) {
      await supabase.from('trip_likes').delete().eq('trip_id', id).eq('user_id', currentUserId);
      setLiked(false); setLikeCount(prev => Math.max(0, prev - 1));
    } else {
      await supabase.from('trip_likes').insert({ trip_id: id, user_id: currentUserId });
      setLiked(true); setLikeCount(prev => prev + 1);
    }
    setLikeLoading(false);
  }

  async function submitComment() {
    if (!commentText.trim() || !currentUserId || commentLoading) return;
    setCommentLoading(true);
    const { error } = await supabase.from('trip_comments').insert({
      trip_id: id, user_id: currentUserId, content: commentText.trim(),
    });
    if (!error) {
      setCommentText('');
      await loadComments();
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 200);
    }
    setCommentLoading(false);
  }

  async function doDeleteComment() {
    if (!confirmDeleteCommentId) return;
    await supabase.from('trip_comments').delete().eq('id', confirmDeleteCommentId);
    setComments(prev => prev.filter(c => c.id !== confirmDeleteCommentId));
    setConfirmDeleteCommentId(null);
  }

  // ── Menú opciones ───────────────────────────────────────────────────────────

  function openMenu() {
    if (Platform.OS === 'ios') {
      ActionSheetIOS.showActionSheetWithOptions(
        {
          options: ['Cancelar', 'Editar viaje', trip.is_published ? 'Ocultar viaje' : 'Publicar viaje', 'Eliminar viaje'],
          destructiveButtonIndex: 3,
          cancelButtonIndex: 0,
        },
        async (index) => {
          if (index === 1) router.push(`/edit-trip?id=${id}` as any);
          if (index === 2) await togglePublish();
          if (index === 3) setConfirmDeleteTrip(true);
        }
      );
    } else {
      setMenuVisible(true);
    }
  }

  async function togglePublish() {
    const newValue = !trip.is_published;
    await supabase.from('trips').update({ is_published: newValue }).eq('id', trip.id);
    setTrip((prev: any) => ({ ...prev, is_published: newValue }));
  }

  async function doDeleteTrip() {
    setConfirmDeleteTrip(false);
    await Promise.all([
      supabase.from('trip_comments').delete().eq('trip_id', id),
      supabase.from('trip_likes').delete().eq('trip_id', id),
      supabase.from('trip_days').delete().eq('trip_id', id),
      supabase.from('cost_items').delete().eq('trip_id', id),
      supabase.from('companions').delete().eq('trip_id', id),
    ]);
    await supabase.from('trips').delete().eq('id', id);
    router.replace('/(tabs)/profile');
  }

  if (loading) return (
    <SafeAreaView style={styles.container}>
      <ActivityIndicator style={{ flex: 1 }} color="#D95F2B" />
    </SafeAreaView>
  );

  const durationDays = trip.start_date && trip.end_date
    ? Math.round((new Date(trip.end_date).getTime() - new Date(trip.start_date).getTime()) / (1000 * 60 * 60 * 24))
    : null;
  const formatDate = (d: string) => new Date(d).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  const formatCommentDate = (d: string) => new Date(d).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  const initials = author?.username ? author.username.slice(0, 2).toUpperCase() : '??';
  const isOwnTrip = currentUserId === trip?.user_id;

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
        <ScrollView ref={scrollRef}>

          {/* Hero */}
          <View style={styles.hero}>
            {trip.cover_photo_url ? (
              <Image source={{ uri: trip.cover_photo_url }} style={StyleSheet.absoluteFillObject} resizeMode="cover" />
            ) : (
              <Text style={styles.heroEmoji}>🌍</Text>
            )}
            <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
              <Text style={styles.backText}>←</Text>
            </TouchableOpacity>
            {isOwnTrip && (
              <TouchableOpacity style={styles.menuBtn} onPress={openMenu}>
                <Text style={styles.menuBtnText}>•••</Text>
              </TouchableOpacity>
            )}
            {isOwnTrip && !trip.is_published && (
              <View style={styles.hiddenBadge}>
                <Text style={styles.hiddenBadgeText}>🔒 Oculto</Text>
              </View>
            )}
          </View>

          <View style={styles.body}>

            {/* Título y precio */}
            <View style={styles.titleRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.destination}>{trip.destination}</Text>
                <Text style={styles.title}>{trip.title}</Text>
                <Text style={styles.dates}>
                  {formatDate(trip.start_date)} – {formatDate(trip.end_date)}
                  {durationDays ? ` · ${durationDays} días` : ''}
                </Text>
              </View>
              <View style={styles.priceBadge}>
                <Text style={styles.priceText}>€{trip.total_cost}</Text>
              </View>
            </View>

            {/* Autor + like */}
            {author && (
              <TouchableOpacity
                style={styles.authorRow}
                onPress={() => !isOwnTrip && router.push(`/user/${author.id}` as any)}
                disabled={isOwnTrip}
                activeOpacity={isOwnTrip ? 1 : 0.7}
              >
                <View style={styles.avatar}>
                  <Text style={styles.avatarText}>{initials}</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.authorName}>{author.username}</Text>
                  <Text style={styles.authorHandle}>@{author.username}</Text>
                </View>
                <TouchableOpacity
                  style={[styles.likeBtn, liked && styles.likeBtnActive]}
                  onPress={toggleLike} disabled={likeLoading}
                >
                  <Text style={styles.likeHeart}>{liked ? '❤️' : '🤍'}</Text>
                  <Text style={[styles.likeCount, liked && styles.likeCountActive]}>{likeCount}</Text>
                </TouchableOpacity>
              </TouchableOpacity>
            )}

            {trip.description && <Text style={styles.description}>{trip.description}</Text>}

            {trip.trip_type && trip.trip_type.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>Tipo de viaje</Text>
                <View style={styles.placesList}>
                  {trip.trip_type.map((t: string, i: number) => (
                    <View key={i} style={styles.placeChip}>
                      <Text style={styles.placeChipText}>{t}</Text>
                    </View>
                  ))}
                </View>
              </View>
            )}

            {!trip.description && (!trip.trip_type || trip.trip_type.length === 0) && (
              <View style={styles.emptySection}>
                <Text style={styles.emptyText}>Este viaje aún no tiene más detalles.</Text>
              </View>
            )}

            {/* Comentarios */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>
                💬 Comentarios{comments.length > 0 ? ` (${comments.length})` : ''}
              </Text>
              {comments.length === 0 ? (
                <View style={styles.emptyComments}>
                  <Text style={styles.emptyCommentsText}>Sé el primero en comentar</Text>
                </View>
              ) : (
                comments.map(c => {
                  const ci = c.users?.username ? c.users.username.slice(0, 2).toUpperCase() : '??';
                  const isMyComment = c.user_id === currentUserId;
                  return (
                    <View key={c.id} style={styles.commentRow}>
                      <View style={styles.commentAvatar}>
                        <Text style={styles.commentAvatarText}>{ci}</Text>
                      </View>
                      <View style={styles.commentBubble}>
                        <View style={styles.commentHeader}>
                          <Text style={styles.commentUsername}>@{c.users?.username}</Text>
                          <Text style={styles.commentDate}>{formatCommentDate(c.created_at)}</Text>
                          {isMyComment && (
                            <TouchableOpacity onPress={() => setConfirmDeleteCommentId(c.id)}>
                              <Text style={styles.commentDelete}>✕</Text>
                            </TouchableOpacity>
                          )}
                        </View>
                        <Text style={styles.commentContent}>{c.content}</Text>
                      </View>
                    </View>
                  );
                })
              )}
            </View>
            <View style={{ height: 16 }} />
          </View>
        </ScrollView>

        {/* Input comentario */}
        <View style={styles.commentInputBar}>
          <TextInput
            style={styles.commentInput}
            value={commentText}
            onChangeText={setCommentText}
            placeholder="Escribe un comentario..."
            placeholderTextColor="#bbb"
            multiline
            maxLength={500}
          />
          <TouchableOpacity
            style={[styles.commentSendBtn, (!commentText.trim() || commentLoading) && styles.commentSendBtnDisabled]}
            onPress={submitComment}
            disabled={!commentText.trim() || commentLoading}
          >
            <Text style={styles.commentSendText}>{commentLoading ? '...' : '↑'}</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>

      {/* Modal menú Android */}
      <Modal visible={menuVisible} transparent animationType="slide" onRequestClose={() => setMenuVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setMenuVisible(false)}>
          <View style={styles.actionSheet}>
            <View style={styles.actionSheetHandle} />
            <TouchableOpacity style={styles.actionItem} onPress={() => { setMenuVisible(false); router.push(`/edit-trip?id=${id}` as any); }}>
              <Text style={styles.actionItemText}>✏️ Editar viaje</Text>
            </TouchableOpacity>
            <View style={styles.actionDivider} />            
            <TouchableOpacity style={styles.actionItem} onPress={() => { setMenuVisible(false); togglePublish(); }}>
              <Text style={styles.actionItemText}>{trip.is_published ? '🔒 Ocultar viaje' : '🌍 Publicar viaje'}</Text>
            </TouchableOpacity>
            <View style={styles.actionDivider} />
            <TouchableOpacity style={styles.actionItem} onPress={() => { setMenuVisible(false); setConfirmDeleteTrip(true); }}>
              <Text style={[styles.actionItemText, styles.actionDestructive]}>🗑️ Eliminar viaje</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.actionItem, styles.actionCancelItem]} onPress={() => setMenuVisible(false)}>
              <Text style={styles.actionCancelText}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Modal confirmar eliminar viaje */}
      <Modal visible={confirmDeleteTrip} transparent animationType="fade" onRequestClose={() => setConfirmDeleteTrip(false)}>
        <View style={styles.modalOverlayCentered}>
          <View style={styles.confirmBox}>
            <Text style={styles.confirmTitle}>¿Eliminar viaje?</Text>
            <Text style={styles.confirmSubtitle}>Esta acción no se puede deshacer. Se borrarán también los comentarios y likes.</Text>
            <TouchableOpacity style={styles.confirmDestructive} onPress={doDeleteTrip}>
              <Text style={styles.confirmDestructiveText}>Sí, eliminar</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.confirmCancel} onPress={() => setConfirmDeleteTrip(false)}>
              <Text style={styles.confirmCancelText}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Modal confirmar eliminar comentario */}
      <Modal visible={!!confirmDeleteCommentId} transparent animationType="fade" onRequestClose={() => setConfirmDeleteCommentId(null)}>
        <View style={styles.modalOverlayCentered}>
          <View style={styles.confirmBox}>
            <Text style={styles.confirmTitle}>¿Eliminar comentario?</Text>
            <Text style={styles.confirmSubtitle}>Esta acción no se puede deshacer.</Text>
            <TouchableOpacity style={styles.confirmDestructive} onPress={doDeleteComment}>
              <Text style={styles.confirmDestructiveText}>Sí, eliminar</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.confirmCancel} onPress={() => setConfirmDeleteCommentId(null)}>
              <Text style={styles.confirmCancelText}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  hero: { height: 200, backgroundColor: '#EAF3DE', alignItems: 'center', justifyContent: 'center' },
  heroEmoji: { fontSize: 72 },
  backBtn: { position: 'absolute', top: 16, left: 16, width: 32, height: 32, borderRadius: 16, backgroundColor: 'rgba(255,255,255,0.85)', alignItems: 'center', justifyContent: 'center' },
  backText: { fontSize: 16, color: '#1a1a1a' },
  menuBtn: { position: 'absolute', top: 16, right: 16, backgroundColor: 'rgba(255,255,255,0.85)', borderRadius: 16, paddingHorizontal: 10, paddingVertical: 6 },
  menuBtnText: { fontSize: 13, color: '#1a1a1a', fontWeight: '700', letterSpacing: 1 },
  hiddenBadge: { position: 'absolute', bottom: 12, right: 12, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 10, paddingHorizontal: 10, paddingVertical: 4 },
  hiddenBadgeText: { fontSize: 11, color: '#fff', fontWeight: '600' },
  body: { padding: 16, gap: 16 },
  titleRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 10 },
  destination: { fontSize: 12, color: '#aaa', marginBottom: 2 },
  title: { fontSize: 18, fontWeight: '800', color: '#1a1a1a', lineHeight: 24 },
  dates: { fontSize: 12, color: '#aaa', marginTop: 4 },
  priceBadge: { backgroundColor: '#D95F2B', borderRadius: 20, paddingHorizontal: 12, paddingVertical: 5 },
  priceText: { color: '#fff', fontSize: 13, fontWeight: '700' },
  authorRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#FAEEDA', alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 12, fontWeight: '800', color: '#633806' },
  authorName: { fontSize: 13, fontWeight: '600', color: '#1a1a1a' },
  authorHandle: { fontSize: 11, color: '#aaa' },
  likeBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, borderWidth: 1, borderColor: '#EAE6E0', backgroundColor: '#fff' },
  likeBtnActive: { borderColor: '#D95F2B', backgroundColor: '#FFF4EF' },
  likeHeart: { fontSize: 16 },
  likeCount: { fontSize: 13, fontWeight: '600', color: '#aaa' },
  likeCountActive: { color: '#D95F2B' },
  description: { fontSize: 14, color: '#555', lineHeight: 22 },
  section: { gap: 10 },
  sectionTitle: { fontSize: 15, fontWeight: '800', color: '#1a1a1a' },
  placesList: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  placeChip: { backgroundColor: '#fff', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 4, borderWidth: 0.5, borderColor: '#B5D4F4' },
  placeChipText: { fontSize: 11, color: '#185FA5' },
  emptySection: { backgroundColor: '#fff', borderRadius: 14, padding: 20, alignItems: 'center', borderWidth: 0.5, borderColor: '#EAE6E0' },
  emptyText: { fontSize: 13, color: '#aaa' },
  emptyComments: { backgroundColor: '#fff', borderRadius: 12, padding: 16, alignItems: 'center', borderWidth: 0.5, borderColor: '#EAE6E0' },
  emptyCommentsText: { fontSize: 13, color: '#bbb' },
  commentRow: { flexDirection: 'row', gap: 8, alignItems: 'flex-start' },
  commentAvatar: { width: 30, height: 30, borderRadius: 15, backgroundColor: '#FAEEDA', alignItems: 'center', justifyContent: 'center', marginTop: 2 },
  commentAvatarText: { fontSize: 10, fontWeight: '800', color: '#633806' },
  commentBubble: { flex: 1, backgroundColor: '#fff', borderRadius: 12, padding: 10, borderWidth: 0.5, borderColor: '#EAE6E0', gap: 4 },
  commentHeader: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  commentUsername: { fontSize: 11, fontWeight: '700', color: '#1a1a1a' },
  commentDate: { fontSize: 10, color: '#ccc', flex: 1 },
  commentDelete: { fontSize: 12, color: '#ddd', paddingHorizontal: 4 },
  commentContent: { fontSize: 13, color: '#444', lineHeight: 18 },
  commentInputBar: { flexDirection: 'row', alignItems: 'flex-end', gap: 8, paddingHorizontal: 12, paddingVertical: 10, backgroundColor: '#fff', borderTopWidth: 0.5, borderTopColor: '#EAE6E0' },
  commentInput: { flex: 1, backgroundColor: '#F8F6F2', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8, fontSize: 14, color: '#1a1a1a', maxHeight: 100, borderWidth: 0.5, borderColor: '#EAE6E0' },
  commentSendBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#D95F2B', alignItems: 'center', justifyContent: 'center' },
  commentSendBtnDisabled: { backgroundColor: '#eee' },
  commentSendText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalOverlayCentered: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center' },
  actionSheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingBottom: 34, paddingTop: 12 },
  actionSheetHandle: { width: 36, height: 4, backgroundColor: '#E0DDD8', borderRadius: 2, alignSelf: 'center', marginBottom: 16 },
  actionItem: { paddingHorizontal: 20, paddingVertical: 16 },
  actionItemText: { fontSize: 16, color: '#1a1a1a' },
  actionDestructive: { color: '#E0443A' },
  actionDivider: { height: 0.5, backgroundColor: '#F0EDE8', marginHorizontal: 20 },
  actionCancelItem: { marginTop: 8, borderTopWidth: 8, borderTopColor: '#F5F2ED' },
  actionCancelText: { fontSize: 16, color: '#888', textAlign: 'center' },
  confirmBox: { backgroundColor: '#fff', borderRadius: 20, margin: 32, padding: 24, gap: 12, alignItems: 'center' },
  confirmTitle: { fontSize: 17, fontWeight: '800', color: '#1a1a1a', textAlign: 'center' },
  confirmSubtitle: { fontSize: 13, color: '#888', textAlign: 'center', lineHeight: 18 },
  confirmDestructive: { width: '100%', backgroundColor: '#E0443A', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  confirmDestructiveText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  confirmCancel: { width: '100%', backgroundColor: '#F5F2ED', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  confirmCancelText: { color: '#1a1a1a', fontSize: 15, fontWeight: '600' },
});
