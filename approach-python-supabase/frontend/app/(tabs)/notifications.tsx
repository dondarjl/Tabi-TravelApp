import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { supabase } from '../../services/supabase';

const avatarColors: Record<string, string> = {
  fav: '#FAEEDA',
  follow: '#EAF3DE',
  like: '#F5F2ED',
  companion: '#EEEDFE',
  comment: '#F5F2ED',
};

const avatarTextColors: Record<string, string> = {
  fav: '#633806',
  follow: '#27500A',
  like: '#888',
  companion: '#3C3489',
  comment: '#888',
};

export default function NotificationsScreen() {
  const [notifs, setNotifs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadNotifs() {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) return;

      const { data, error } = await supabase
        .from('notifications')
        .select('*')
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });

      if (!error && data) setNotifs(data);
      setLoading(false);
    }

    loadNotifs();
  }, []);

  if (loading) return (
    <SafeAreaView style={styles.container}>
      <ActivityIndicator style={{ flex: 1 }} color="#D95F2B" />
    </SafeAreaView>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.logo}>notificaciones</Text>
      </View>

      {notifs.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyEmoji}>🔔</Text>
          <Text style={styles.emptyTitle}>Sin notificaciones</Text>
          <Text style={styles.emptySubtitle}>Cuando alguien interactúe contigo aparecerá aquí</Text>
        </View>
      ) : (
        <FlatList
          data={notifs}
          keyExtractor={item => item.id}
          renderItem={({ item }) => (
            <View style={styles.item}>
              <View style={[styles.avatar, { backgroundColor: avatarColors[item.type] || '#F5F2ED' }]}>
                <Text style={[styles.avatarText, { color: avatarTextColors[item.type] || '#888' }]}>
                  {item.type === 'follow' ? '+' : item.type === 'fav' ? '★' : '•'}
                </Text>
              </View>
              <View style={styles.itemBody}>
                <Text style={styles.itemText}>{item.message || item.text}</Text>
                <Text style={styles.itemTime}>{new Date(item.created_at).toLocaleDateString('es-ES')}</Text>
              </View>
              {!item.is_read && <View style={styles.dot} />}
            </View>
          )}
          ItemSeparatorComponent={() => <View style={styles.separator} />}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  header: { paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 0.5, borderBottomColor: '#F0EDE8' },
  logo: { fontSize: 20, fontWeight: '800', color: '#1a1a1a' },
  item: { flexDirection: 'row', alignItems: 'flex-start', padding: 14, gap: 10 },
  avatar: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  avatarText: { fontSize: 12, fontWeight: '700' },
  itemBody: { flex: 1 },
  itemText: { fontSize: 13, color: '#1a1a1a', lineHeight: 18 },
  itemTime: { fontSize: 11, color: '#bbb', marginTop: 3 },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#D95F2B', marginTop: 4, flexShrink: 0 },
  separator: { height: 0.5, backgroundColor: '#F0EDE8', marginLeft: 60 },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 8, padding: 40 },
  emptyEmoji: { fontSize: 40 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: '#1a1a1a' },
  emptySubtitle: { fontSize: 13, color: '#aaa', textAlign: 'center', lineHeight: 20 },
});