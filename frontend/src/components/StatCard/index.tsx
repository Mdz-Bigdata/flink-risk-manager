import React from 'react';
import { Card, Statistic } from 'antd';
import { useStyles } from './StatCard.styles';

interface StatCardProps {
  title: string;
  value: number | string;
  icon?: React.ReactNode;
  valueStyle?: React.CSSProperties;
  trend?: 'up' | 'down' | 'flat';
  loading?: boolean;
  onClick?: () => void;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, valueStyle, trend, loading, onClick }) => {
  const { styles } = useStyles();
  return (
    <Card
      className={onClick ? styles.clickableCard : styles.card}
      loading={loading}
      bordered={false}
      onClick={onClick}
    >
      <div className={styles.content}>
        <div className={styles.info}>
          <Statistic title={title} value={value} valueStyle={valueStyle} />
        </div>
        {icon && <div className={styles.icon}>{icon}</div>}
      </div>
    </Card>
  );
};

export default StatCard;
