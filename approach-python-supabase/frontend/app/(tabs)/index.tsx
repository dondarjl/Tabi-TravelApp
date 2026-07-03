import { supabase } from '@/services/supabase';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, Image, SafeAreaView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
export default function FeedScreen() {
  const router = useRouter();
  const [trips, setTrips] = useState<any[]>([]);
  const [likeCounts, setLikeCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);

  // useFocusEffect para refrescar al volver desde un viaje
  useFocusEffect(
    useCallback(() => {
      loadFeed();
    }, [])
  );

  async function loadFeed() {
    setLoading(true);
    const { data, error } = await supabase
      .from('trips')
      .select('*')
      .eq('is_published', true)
      .order('created_at', { ascending: false });

    if (!error && data) {
      setTrips(data);
      loadLikeCounts(data.map((t: any) => t.id));
    }
    setLoading(false);
  }

  async function loadLikeCounts(tripIds: string[]) {
    if (tripIds.length === 0) return;

    // Trae todos los likes de los viajes del feed en una sola query
    const { data } = await supabase
      .from('trip_likes')
      .select('trip_id')
      .in('trip_id', tripIds);

    if (!data) return;

    const counts: Record<string, number> = {};
    for (const row of data) {
      counts[row.trip_id] = (counts[row.trip_id] ?? 0) + 1;
    }
    setLikeCounts(counts);
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.logo}>tabi</Text>
        </View>
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
          <ActivityIndicator color="#D95F2B" />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.logo}>tabi</Text>
        <TouchableOpacity
          style={styles.postBtn}
          onPress={() => router.push('/post' as any)}
        >
          <Text style={styles.postBtnText}>+</Text>
        </TouchableOpacity>
      </View>
      {trips.length === 0 ? (
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <Text style={{ fontSize: 32 }}>✈️</Text>
          <Text style={{ fontSize: 16, fontWeight: '700', color: '#1a1a1a' }}>Aún no hay viajes</Text>
          <Text style={{ fontSize: 13, color: '#aaa' }}>Sé el primero en publicar</Text>
        </View>
      ) : (
        <FlatList
          data={trips}
          keyExtractor={(item: any) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }: any) => (
            <TouchableOpacity onPress={() => router.push(`/trip/${item.id}` as any)}>
              <View style={styles.card}>
                <View style={styles.cardImage}>
                  {item.cover_photo_url ? (
                    <Image
                      source={{ uri: item.cover_photo_url }}
                      style={{ width: '100%', height: '100%' }}
                      resizeMode="cover"
                    />
                  ) : (
                    <Text style={styles.emoji}>🌍</Text>
                  )}
                </View>
                <View style={styles.cardBody}>
                  <Text style={styles.destination}>{item.destination}</Text>
                  <Text style={styles.title}>{item.title}</Text>
                  <View style={styles.meta}>
                    <View style={styles.priceBadge}>
                      <Text style={styles.priceText}>€{item.total_cost}</Text>
                    </View>
                    <Text style={styles.duration}>{item.start_date} – {item.end_date}</Text>
                    <View style={styles.likesBadge}>
                      <Text style={styles.likesText}>🤍 {likeCounts[item.id] ?? 0}</Text>
                    </View>
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  header: { paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 0.5, borderBottomColor: '#E0DDD8', backgroundColor: '#fff', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  logo: { fontSize: 24, fontWeight: '800', color: '#1a1a1a', letterSpacing: -0.5 },
  postBtn: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#D95F2B', alignItems: 'center', justifyContent: 'center' },
  postBtnText: { color: '#fff', fontSize: 20, fontWeight: '300', lineHeight: 32 },
  list: { padding: 12, gap: 12 },
  card: { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 0.5, borderColor: '#EAE6E0' },
  cardImage: { height: 160, backgroundColor: '#F0EDE8', alignItems: 'center', justifyContent: 'center' },
  emoji: { fontSize: 64 },
  cardBody: { padding: 14, gap: 6 },
  destination: { fontSize: 12, color: '#aaa' },
  title: { fontSize: 16, fontWeight: '700', color: '#1a1a1a', lineHeight: 22 },
  meta: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 4 },
  priceBadge: { backgroundColor: '#D95F2B', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 3 },
  priceText: { color: '#fff', fontSize: 12, fontWeight: '600' },
  duration: { fontSize: 12, color: '#aaa' },
  likesBadge: { marginLeft: 'auto' as any },
  likesText: { fontSize: 12, color: '#aaa' },
});
