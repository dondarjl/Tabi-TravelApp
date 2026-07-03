import { Tabs } from 'expo-router';
import { Text, View } from 'react-native';

function TabIcon({ label, active }: { label: string; active: boolean }) {
  return (
    <View style={{ alignItems: 'center', gap: 2 }}>
      <View style={{
        width: 6, height: 6, borderRadius: 3,
        backgroundColor: active ? '#D95F2B' : 'transparent'
      }} />
      <Text style={{ fontSize: 10, color: active ? '#1a1a1a' : '#ccc' }}>{label}</Text>
    </View>
  );
}

export default function Layout() {
  return (
    <Tabs screenOptions={{
      headerShown: false,
      tabBarStyle: {
        backgroundColor: '#fff',
        borderTopWidth: 0.5,
        borderTopColor: '#F0EDE8',
        height: 60,
        paddingBottom: 8,
      },
      tabBarShowLabel: false,
    }}>
      <Tabs.Screen
        name="index"
        options={{
          tabBarIcon: ({ focused }) => <TabIcon label="Inicio" active={focused} />
        }}
      />
      <Tabs.Screen
        name="discover"
        options={{
          tabBarIcon: ({ focused }) => <TabIcon label="Descubrir" active={focused} />
        }}
      />
      <Tabs.Screen
        name="notifications"
        options={{
          tabBarIcon: ({ focused }) => <TabIcon label="Avisos" active={focused} />
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          tabBarIcon: ({ focused }) => <TabIcon label="Perfil" active={focused} />
        }}
      />
    </Tabs>
  );
}