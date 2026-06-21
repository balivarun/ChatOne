'use client';
import { useRouter, usePathname } from 'next/navigation';
import { Users } from 'lucide-react';
import { Group } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { getInitials, cn } from '@/lib/utils';

interface Props {
  group: Group;
}

export default function GroupItem({ group }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const isActive = pathname === `/group/${group.id}`;

  const handleClick = () => {
    router.push(`/group/${group.id}`);
  };

  return (
    <div
      onClick={handleClick}
      className={cn(
        'flex items-center gap-3 px-3 py-3 cursor-pointer hover:bg-black/5 dark:hover:bg-white/5 transition-colors',
        isActive && 'bg-black/8 dark:bg-white/8'
      )}
    >
      <div className="relative flex-shrink-0">
        <Avatar className="w-10 h-10">
          <AvatarImage src={group.avatarUrl} alt={group.name} />
          <AvatarFallback className="bg-accent/20 text-accent font-semibold text-sm">
            {getInitials(group.name)}
          </AvatarFallback>
        </Avatar>
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-1">
          <span className="text-sm font-medium truncate">{group.name}</span>
          <span className="text-xs text-gray-400 flex items-center gap-1">
            <Users className="w-3 h-3" />
            {group.memberCount}
          </span>
        </div>
        {group.description && (
          <p className="text-xs text-gray-500 truncate mt-0.5">
            {group.description}
          </p>
        )}
        <p className="text-xs text-gray-400 mt-0.5">
          {group.role === 'ADMIN' ? 'Admin' : 'Member'}
        </p>
      </div>
    </div>
  );
}
