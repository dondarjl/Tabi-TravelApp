import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Image, SafeAreaView, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { supabase } from '../../services/supabase';

export default function ProfileScreen() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);
  const [trips, setTrips] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);

  useFocusEffect(
    useCallback(() => {
      loadProfile();
    }, [])
  );

  async function loadProfile() {
    const { data: { user: authUser } } = await supabase.auth.getUser();
    if (!authUser) return;

    const { data: userData } = await supabase
      .from('users')
      .select('*')
      .eq('id', authUser.id)
      .single();

    const { data: tripsData } = await supabase
      .from('trips')
      .select('*')
      .eq('user_id', authUser.id)
      .order('created_at', { ascending: false });

    // Contadores reales de follows
    const [{ count: followers }, { count: following }] = await Promise.all([
      supabase.from('follows').select('*', { count: 'exact', head: true }).eq('following_id', authUser.id),
      supabase.from('follows').select('*', { count: 'exact', head: true }).eq('follower_id', authUser.id),
    ]);

    setUser(userData);
    setTrips(tripsData || []);
    setFollowerCount(followers ?? 0);
    setFollowingCount(following ?? 0);
    setLoading(false);
  }

  if (loading) return (
    <SafeAreaView style={styles.container}>
      <ActivityIndicator style={{ flex: 1 }} color="#D95F2B" />
    </SafeAreaView>
  );

  const initials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : '??';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView>
        <View style={styles.header}>
          <View style={styles.topRow}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{initials}</Text>
            </View>
            <View style={styles.headerInfo}>
              <Text style={styles.name}>{user?.username || 'Sin nombre'}</Text>
              <Text style={styles.handle}>@{user?.username || ''}</Text>
            </View>
            <TouchableOpacity
              style={styles.editBtn}
              onPress={() => router.push('/edit-profile' as any)}
            >
              <Text style={styles.editText}>Editar</Text>
            </TouchableOpacity>
            {/* Botón ajustes — solo accesible desde aquí */}
            <TouchableOpacity
              style={styles.settingsBtn}
              onPress={() => router.push('/settings' as any)}
            >
              <Text style={styles.settingsIcon}>⚙️</Text>
            </TouchableOpacity>
          </View>

          {user?.bio && (
            <Text style={styles.bio}>"{user.bio}"</Text>
          )}

          <View style={styles.statsRow}>
            {[
              { label: 'viajes', value: trips.length.toString() },
              { label: 'seguidores', value: followerCount.toString() },
              { label: 'siguiendo', value: followingCount.toString() },
            ].map(s => (
              <View key={s.label} style={styles.stat}>
                <Text style={styles.statNum}>{s.value}</Text>
                <Text style={styles.statLabel}>{s.label}</Text>
              </View>
            ))}
          </View>
        </View>

        <View style={styles.mapPreview}>
          <Text style={styles.mapText}>Mapa de viajes · {trips.length} destinos</Text>
        </View>

        {trips.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyEmoji}>✈️</Text>
            <Text style={styles.emptyTitle}>Aún no has publicado viajes</Text>
            <TouchableOpacity
              style={styles.newTripBtn}
              onPress={() => router.push('/post' as any)}
            >
              <Text style={styles.newTripText}>+ Publicar viaje</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.grid}>
            {trips.map(t => (
              <TouchableOpacity
                key={t.id}
                style={styles.gridCell}
                onPress={() => router.push(`/trip/${t.id}` as any)}
              >
                {t.cover_photo_url ? (
                  <Image
                    source={{ uri: t.cover_photo_url }}
                    style={styles.gridImage}
                    resizeMode="cover"
                  />
                ) : (
                  <Text style={styles.gridEmoji}>🌍</Text>
                )}
                <Text style={styles.gridDestination} numberOfLines={1}>{t.destination}</Text>
              </TouchableOpacity>
            ))}
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  header: { backgroundColor: '#fff', padding: 16, borderBottomWidth: 0.5, borderBottomColor: '#F0EDE8' },
  topRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 10 },
  avatar: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#FAEEDA', alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 16, fontWeight: '800', color: '#633806' },
  headerInfo: { flex: 1 },
  name: { fontSize: 16, fontWeight: '800', color: '#1a1a1a' },
  handle: { fontSize: 12, color: '#aaa' },
  editBtn: { borderWidth: 1, borderColor: '#D95F2B', borderRadius: 20, paddingHorizontal: 12, paddingVertical: 4 },
  editText: { fontSize: 11, color: '#D95F2B' },
  settingsBtn: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#F5F2ED', alignItems: 'center', justifyContent: 'center' },
  settingsIcon: { fontSize: 16 },
  bio: { fontSize: 12, color: '#888', fontStyle: 'italic', lineHeight: 18, marginBottom: 12 },
  statsRow: { flexDirection: 'row', gap: 6 },
  stat: { flex: 1, backgroundColor: '#F5F2ED', borderRadius: 10, padding: 8, alignItems: 'center' },
  statNum: { fontSize: 15, fontWeight: '800', color: '#1a1a1a' },
  statLabel: { fontSize: 9, color: '#bbb', marginTop: 1 },
  mapPreview: { margin: 12, backgroundColor: '#E6F1FB', borderRadius: 14, height: 90, alignItems: 'center', justifyContent: 'center', borderWidth: 0.5, borderColor: '#B5D4F4' },
  mapText: { fontSize: 12, color: '#185FA5', fontWeight: '500' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', padding: 8, gap: 4 },
  gridCell: { width: '31.5%', aspectRatio: 1, backgroundColor: '#fff', borderRadius: 10, alignItems: 'center', justifyContent: 'center', borderWidth: 0.5, borderColor: '#EAE6E0', gap: 4, padding: 8 },
  gridEmoji: { fontSize: 24 },
  gridDestination: { fontSize: 9, color: '#888', textAlign: 'center' },
  emptyState: { padding: 40, alignItems: 'center', gap: 8 },
  emptyEmoji: { fontSize: 40 },
  emptyTitle: { fontSize: 14, color: '#aaa' },
  newTripBtn: { marginTop: 8, backgroundColor: '#D95F2B', borderRadius: 20, paddingHorizontal: 20, paddingVertical: 8 },
  newTripText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  gridImage: { width: '100%', height: 60, borderRadius: 8 },
});
