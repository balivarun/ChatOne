'use client';
import { useState, useEffect, useCallback } from 'react';
import {
  Shield,
  UserMinus,
  UserPlus,
  Trash2,
  Upload,
  Search,
} from 'lucide-react';
import { Group, GroupMember, User } from '@/types';
import { groupApi, userApi } from '@/lib/api';
import UserAvatar from '@/components/common/UserAvatar';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { getInitials } from '@/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import toast from 'react-hot-toast';
import { useAuthStore } from '@/store/authStore';
import { useRouter } from 'next/navigation';
import { useDropzone } from 'react-dropzone';

interface Props {
  group: Group;
  onGroupUpdated: (updated: Partial<Group>) => void;
}

export default function GroupInfoPanel({ group, onGroupUpdated }: Props) {
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddMember, setShowAddMember] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [searching, setSearching] = useState(false);
  const { user } = useAuthStore();
  const router = useRouter();
  const isAdmin = group.role === 'ADMIN';

  const loadMembers = useCallback(async () => {
    try {
      const { data } = await groupApi.getMembers(group.id);
      setMembers(data.data || data);
    } catch {
      toast.error('Failed to load members');
    } finally {
      setLoading(false);
    }
  }, [group.id]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadMembers();
  }, [loadMembers]);

  const onAvatarDrop = useCallback(
    async (files: File[]) => {
      const file = files[0];
      if (!file) return;
      try {
        const { data } = await groupApi.updateAvatar(group.id, file);
        const updated = data.data || data;
        onGroupUpdated({ avatarUrl: updated.avatarUrl });
        toast.success('Group photo updated');
      } catch {
        toast.error('Failed to update photo');
      }
    },
    [group.id, onGroupUpdated]
  );

  const { getInputProps: getAvatarInputProps, open: openAvatarPicker } =
    useDropzone({
      onDrop: onAvatarDrop,
      accept: { 'image/*': [] },
      maxFiles: 1,
      noClick: true,
      noKeyboard: true,
    });

  const handleRemoveMember = async (userId: string) => {
    if (!confirm('Remove this member?')) return;
    try {
      await groupApi.removeMember(group.id, userId);
      setMembers((prev) => prev.filter((m) => m.user.id !== userId));
      toast.success('Member removed');
    } catch {
      toast.error('Failed to remove member');
    }
  };

  const handlePromote = async (userId: string) => {
    try {
      await groupApi.promoteToAdmin(group.id, userId);
      setMembers((prev) =>
        prev.map((m) =>
          m.user.id === userId ? { ...m, role: 'ADMIN' } : m
        )
      );
      toast.success('Promoted to admin');
    } catch {
      toast.error('Failed to promote member');
    }
  };

  const handleSearchUsers = async (q: string) => {
    setSearchQuery(q);
    if (!q.trim()) {
      setSearchResults([]);
      return;
    }
    setSearching(true);
    try {
      const { data } = await userApi.searchUsers(q);
      const users: User[] = data.data?.content || data.content || data.data || [];
      setSearchResults(
        users.filter((u) => !members.find((m) => m.user.id === u.id))
      );
    } catch {
      toast.error('Search failed');
    } finally {
      setSearching(false);
    }
  };

  const handleAddMember = async (userId: string) => {
    try {
      await groupApi.addMembers(group.id, [userId]);
      await loadMembers();
      setSearchResults([]);
      setSearchQuery('');
      setShowAddMember(false);
      toast.success('Member added');
    } catch {
      toast.error('Failed to add member');
    }
  };

  const handleDeleteGroup = async () => {
    if (!confirm(`Delete group "${group.name}"? This cannot be undone.`)) return;
    try {
      await groupApi.delete(group.id);
      toast.success('Group deleted');
      router.push('/chat');
    } catch {
      toast.error('Failed to delete group');
    }
  };

  return (
    <div className="flex flex-col h-full bg-sidebar overflow-y-auto">
      <input {...getAvatarInputProps()} />

      {/* Header */}
      <div className="flex flex-col items-center gap-3 p-6 border-b border-black/10 dark:border-white/10">
        <div className="relative">
          <Avatar className="w-24 h-24">
            <AvatarImage src={group.avatarUrl} alt={group.name} />
            <AvatarFallback className="bg-accent/20 text-accent font-bold text-2xl">
              {getInitials(group.name)}
            </AvatarFallback>
          </Avatar>
          {isAdmin && (
            <button
              onClick={openAvatarPicker}
              className="absolute bottom-0 right-0 bg-accent text-white rounded-full p-1 shadow-md hover:bg-accent/90"
            >
              <Upload className="w-3 h-3" />
            </button>
          )}
        </div>
        <div className="text-center">
          <h2 className="text-xl font-bold">{group.name}</h2>
          {group.description && (
            <p className="text-sm text-gray-500 mt-1">{group.description}</p>
          )}
          <p className="text-xs text-gray-400 mt-1">
            {group.memberCount} members
          </p>
        </div>
      </div>

      {/* Members */}
      <div className="p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold">Members</h3>
          {isAdmin && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowAddMember(!showAddMember)}
            >
              <UserPlus className="w-4 h-4 mr-1" />
              Add
            </Button>
          )}
        </div>

        {showAddMember && (
          <div className="mb-3 space-y-2">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                value={searchQuery}
                onChange={(e) => handleSearchUsers(e.target.value)}
                placeholder="Search users"
                className="pl-9 text-sm"
              />
            </div>
            {searchResults.length > 0 && (
              <div className="border border-gray-200 dark:border-gray-700 rounded-lg max-h-32 overflow-y-auto">
                {searchResults.map((u) => (
                  <div
                    key={u.id}
                    onClick={() => handleAddMember(u.id)}
                    className="flex items-center gap-2 px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer"
                  >
                    <UserAvatar user={u} size="sm" />
                    <span className="text-sm">{u.displayName}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {loading ? (
          <div className="flex justify-center py-4">
            <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-accent" />
          </div>
        ) : (
          <div className="space-y-1">
            {members.map((member) => (
              <div
                key={member.user.id}
                className="flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/5"
              >
                <UserAvatar user={member.user} size="sm" showStatus />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1">
                    <p className="text-sm font-medium truncate">
                      {member.user.displayName}
                      {member.user.id === user?.id && ' (You)'}
                    </p>
                    {member.role === 'ADMIN' && (
                      <Shield className="w-3 h-3 text-accent flex-shrink-0" />
                    )}
                  </div>
                  <p className="text-xs text-gray-400">{member.role}</p>
                </div>

                {isAdmin && member.user.id !== user?.id && (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-7 w-7">
                        <span className="text-gray-400">•••</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      {member.role !== 'ADMIN' && (
                        <DropdownMenuItem
                          onClick={() => handlePromote(member.user.id)}
                        >
                          <Shield className="mr-2 h-4 w-4" />
                          Make Admin
                        </DropdownMenuItem>
                      )}
                      <DropdownMenuItem
                        onClick={() => handleRemoveMember(member.user.id)}
                        className="text-red-500 focus:text-red-500"
                      >
                        <UserMinus className="mr-2 h-4 w-4" />
                        Remove
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Danger zone */}
      {isAdmin && (
        <div className="p-4 mt-auto border-t border-black/10 dark:border-white/10">
          <Button
            variant="destructive"
            className="w-full"
            onClick={handleDeleteGroup}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            Delete Group
          </Button>
        </div>
      )}
    </div>
  );
}
