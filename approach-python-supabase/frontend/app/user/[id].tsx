import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Image, SafeAreaView, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { supabase } from '../../services/supabase';

export default function TripScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams();
  const [trip, setTrip] = useState<any>(null);
  const [author, setAuthor] = useState<any>(null);
  const [days, setDays] = useState<any[]>([]);
  const [costs, setCosts] = useState<any[]>([]);
  const [companions, setCompanions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [likeLoading, setLikeLoading] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);

  useEffect(() => {
    loadTrip();
  }, [id]);

  async function loadTrip() {
    const { data, error } = await supabase
      .from('trips')
      .select('*')
      .eq('id', id)
      .single();

    if (error || !data) { router.back(); return; }
    setTrip(data);

    // Autor
    const { data: userData } = await supabase
      .from('users')
      .select('*')
      .eq('id', data.user_id)
      .single();
    setAuthor(userData);

    // Itinerario
    const { data: daysData } = await supabase
      .from('trip_days')
      .select('*')
      .eq('trip_id', id)
      .order('day_number', { ascending: true });
    setDays(daysData || []);

    // Costes
    const { data: costsData } = await supabase
      .from('cost_items')
      .select('*')
      .eq('trip_id', id)
      .order('amount', { ascending: false });
    setCosts(costsData || []);

    // Compañeros
    const { data: compData } = await supabase
      .from('companions')
      .select('user_id, users(id, username)')
      .eq('trip_id', id);
    setCompanions(compData || []);

    // Sesión + likes
    const { data: session } = await supabase.auth.getSession();
    const userId = session?.session?.user?.id ?? null;
    setCurrentUserId(userId);

    const { count } = await supabase
      .from('trip_likes')
      .select('*', { count: 'exact', head: true })
      .eq('trip_id', id);
    setLikeCount(count ?? 0);

    if (userId) {
      const { data: myLike } = await supabase
        .from('trip_likes')
        .select('id')
        .eq('trip_id', id)
        .eq('user_id', userId)
        .maybeSingle();
      setLiked(!!myLike);
    }

    setLoading(false);
  }

  async function toggleLike() {
    if (likeLoading || !currentUserId) return;
    setLikeLoading(true);
    if (liked) {
      await supabase.from('trip_likes').delete().eq('trip_id', id).eq('user_id', currentUserId);
      setLiked(false);
      setLikeCount(prev => Math.max(0, prev - 1));
    } else {
      await supabase.from('trip_likes').insert({ trip_id: id, user_id: currentUserId });
      setLiked(true);
      setLikeCount(prev => prev + 1);
    }
    setLikeLoading(false);
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
  const formatDayDate = (d: string) => new Date(d).toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' });
  const initials = author?.username ? author.username.slice(0, 2).toUpperCase() : '??';
  const isOwnTrip = currentUserId === trip?.user_id;
  const totalCosts = costs.reduce((sum, c) => sum + (c.amount || 0), 0);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView>
        {/* Hero */}
        <View style={styles.hero}>
          {trip.cover_photo_url ? (
            <Image
              source={{ uri: trip.cover_photo_url }}
              style={StyleSheet.absoluteFillObject}
              resizeMode="cover"
            />
          ) : (
            <Text style={styles.heroEmoji}>🌍</Text>
          )}
          <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
            <Text style={styles.backText}>←</Text>
          </TouchableOpacity>
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
                onPress={toggleLike}
                disabled={likeLoading}
              >
                <Text style={styles.likeHeart}>{liked ? '❤️' : '🤍'}</Text>
                <Text style={[styles.likeCount, liked && styles.likeCountActive]}>{likeCount}</Text>
              </TouchableOpacity>
            </TouchableOpacity>
          )}

          {/* Chips tipo viaje */}
          {trip.trip_type && trip.trip_type.length > 0 && (
            <View style={styles.chips}>
              {trip.trip_type.map((t: string, i: number) => (
                <View key={i} style={styles.chip}>
                  <Text style={styles.chipText}>{t}</Text>
                </View>
              ))}
            </View>
          )}

          {/* Descripción */}
          {trip.description && (
            <Text style={styles.description}>{trip.description}</Text>
          )}

          {/* ── COMPAÑEROS ─────────────────────────────────────────────────── */}
          {companions.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>👥 Viajaron juntos</Text>
              <View style={styles.companionsRow}>
                {companions.map((c: any) => {
                  const u = c.users;
                  if (!u) return null;
                  return (
                    <TouchableOpacity
                      key={c.user_id}
                      style={styles.companionChip}
                      onPress={() => router.push(`/user/${u.id}` as any)}
                    >
                      <View style={styles.companionAvatar}>
                        <Text style={styles.companionAvatarText}>
                          {u.username.slice(0, 2).toUpperCase()}
                        </Text>
                      </View>
                      <Text style={styles.companionName}>@{u.username}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>
          )}

          {/* ── ITINERARIO ─────────────────────────────────────────────────── */}
          {days.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>📅 Itinerario</Text>
              {days.map((day) => (
                <View key={day.id} style={styles.dayCard}>
                  <Text style={styles.dayTitle}>
                    Día {day.day_number} · {formatDayDate(day.day_date)}
                  </Text>
                  {day.morning_description && (
                    <View style={styles.dayBlock}>
                      <Text style={styles.dayPeriod}>🌅 Mañana</Text>
                      <Text style={styles.dayText}>{day.morning_description}</Text>
                    </View>
                  )}
                  {day.afternoon_description && (
                    <View style={styles.dayBlock}>
                      <Text style={styles.dayPeriod}>🌆 Tarde</Text>
                      <Text style={styles.dayText}>{day.afternoon_description}</Text>
                    </View>
                  )}
                </View>
              ))}
            </View>
          )}

          {/* ── DESGLOSE DE COSTES ─────────────────────────────────────────── */}
          {costs.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>💸 Desglose de costes</Text>
              <View style={styles.costsCard}>
                {costs.map((c, i) => (
                  <View key={c.id}>
                    <View style={styles.costRow}>
                      <Text style={styles.costCategory}>{c.category}</Text>
                      <Text style={styles.costAmount}>€{c.amount}</Text>
                    </View>
                    {i < costs.length - 1 && <View style={styles.costDivider} />}
                  </View>
                ))}
                <View style={styles.costTotalRow}>
                  <Text style={styles.costTotalLabel}>Total desglosado</Text>
                  <Text style={styles.costTotalAmount}>€{totalCosts}</Text>
                </View>
              </View>
            </View>
          )}

          {/* Vacío */}
          {!trip.description && (!trip.trip_type || trip.trip_type.length === 0) && days.length === 0 && costs.length === 0 && companions.length === 0 && (
            <View style={styles.emptySection}>
              <Text style={styles.emptyText}>Este viaje aún no tiene más detalles.</Text>
            </View>
          )}

        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  hero: { height: 200, backgroundColor: '#EAF3DE', alignItems: 'center', justifyContent: 'center' },
  heroEmoji: { fontSize: 72 },
  backBtn: { position: 'absolute', top: 16, left: 16, width: 32, height: 32, borderRadius: 16, backgroundColor: 'rgba(255,255,255,0.85)', alignItems: 'center', justifyContent: 'center' },
  backText: { fontSize: 16, color: '#1a1a1a' },
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
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip: { backgroundColor: '#fff', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 4, borderWidth: 0.5, borderColor: '#B5D4F4' },
  chipText: { fontSize: 11, color: '#185FA5' },
  description: { fontSize: 14, color: '#555', lineHeight: 22 },
  section: { gap: 10 },
  sectionTitle: { fontSize: 15, fontWeight: '800', color: '#1a1a1a' },
  companionsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  companionChip: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#fff', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 6, borderWidth: 0.5, borderColor: '#EAE6E0' },
  companionAvatar: { width: 24, height: 24, borderRadius: 12, backgroundColor: '#FAEEDA', alignItems: 'center', justifyContent: 'center' },
  companionAvatarText: { fontSize: 9, fontWeight: '800', color: '#633806' },
  companionName: { fontSize: 12, color: '#1a1a1a', fontWeight: '500' },
  dayCard: { backgroundColor: '#fff', borderRadius: 14, padding: 14, gap: 10, borderWidth: 0.5, borderColor: '#EAE6E0' },
  dayTitle: { fontSize: 12, fontWeight: '800', color: '#D95F2B' },
  dayBlock: { gap: 4 },
  dayPeriod: { fontSize: 11, fontWeight: '700', color: '#888' },
  dayText: { fontSize: 13, color: '#444', lineHeight: 20 },
  costsCard: { backgroundColor: '#fff', borderRadius: 14, overflow: 'hidden', borderWidth: 0.5, borderColor: '#EAE6E0' },
  costRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 14, paddingVertical: 11 },
  costCategory: { fontSize: 13, color: '#555' },
  costAmount: { fontSize: 13, fontWeight: '600', color: '#1a1a1a' },
  costDivider: { height: 0.5, backgroundColor: '#F0EDE8', marginHorizontal: 14 },
  costTotalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 14, paddingVertical: 12, backgroundColor: '#F8F6F2', borderTopWidth: 1, borderTopColor: '#EAE6E0' },
  costTotalLabel: { fontSize: 13, fontWeight: '700', color: '#1a1a1a' },
  costTotalAmount: { fontSize: 15, fontWeight: '800', color: '#D95F2B' },
  emptySection: { backgroundColor: '#fff', borderRadius: 14, padding: 20, alignItems: 'center', borderWidth: 0.5, borderColor: '#EAE6E0' },
  emptyText: { fontSize: 13, color: '#aaa' },
});
